package server.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.Gson;
import shared.utils.GsonUtils;

import server.service.AuctionService;
import server.service.StorageService;
import server.service.UserService;
import shared.enums.ItemStatus;
import shared.models.Art;
import shared.models.Auction;
import shared.models.Collectible;
import shared.models.Electronic;
import shared.models.Fashion;
import shared.models.Item;
import shared.models.Seller;
import shared.models.User;
import shared.models.Vehicle;
import shared.network.Request;
import shared.network.Response;

public class CreateAuctionHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final UserService userService;
    private final StorageService storageService;
    private final Gson gson = GsonUtils.createGson();

    public CreateAuctionHandler(AuctionService auctionService, UserService userService, StorageService storageService) {
        this.auctionService = auctionService;
        this.userService = userService;
        this.storageService = storageService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        try {
            var data = request.getData();
            String username = data.get("username");

            // Check if user is banned
            if (userService.isBanned(username)) {
                return new Response("FAIL", "Your account has been banned. You cannot perform this action.");
            }

            String name = data.get("name");
            String desc = data.get("description");
            BigDecimal price = new BigDecimal(data.get("price")).setScale(2, RoundingMode.UP);

            Instant start = Instant.parse(data.get("startTime"));
            Instant end = Instant.parse(data.get("endTime"));
            String category = data.get("category");
            String imageBase64 = data.get("image");

            User u = userService.getUser(username);

            if (u == null || !(u instanceof Seller)) {
                return new Response("FAIL", "Invalid user");
            }

            Seller seller = (Seller) u;
            System.out.println("seller = " + seller);

            String imageUrl = null;
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                String imageId = UUID.randomUUID().toString();
                String contentType = data.get("imageContentType");
                imageUrl = storageService.uploadFile(imageId, imageBytes, contentType);
            }

            Item item = null;
            if (category.equals("COLLECTIBLES")) {
                int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                item = new Collectible(name, desc, seller, price, year);
            } else if (category.equals("ELECTRONICS")) {
                String brand = data.getOrDefault("brandField", "Default");
                ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                item = new Electronic(name, desc, seller, price, brand, status);
            } else if (category.equals("ARTS")) {
                String artist = data.getOrDefault("artistField", "Unknown");
                int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                boolean original = Boolean.parseBoolean(data.getOrDefault("originalBox", "false"));
                item = new Art(name, desc, seller, price, artist, year, original);
            } else if (category.equals("VEHICLES")) {
                String brand = data.getOrDefault("brandField", "Unknown");
                int model = Integer.parseInt(data.getOrDefault("modelField", "0"));
                int km = Integer.parseInt(data.getOrDefault("kmField", "0"));
                item = new Vehicle(name, desc, seller, price, brand, model, km);
            } else if (category.equals("FASHIONS")) {
                String brand = data.getOrDefault("brandField", "Brand");
                ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                item = new Fashion(name, desc, seller, price, brand, status);
            }

            if (item != null) {
                item.setImageUrl(imageUrl);
                
                String incType = data.get("incrementType");
                BigDecimal minInc;
                BigDecimal defaultMinInc = price.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.UP);
                if ("Custom Amount".equals(incType)) {
                    minInc = new BigDecimal(data.get("minIncrement")).setScale(2, RoundingMode.UP);
                    if (minInc.compareTo(defaultMinInc) < 0) {
                        return new Response("FAIL", "Custom increment must be greater than or equal to default minimum increment: " + defaultMinInc.toPlainString());
                    }
                } else {
                    minInc = defaultMinInc;
                }

                item.setMinIncrement(minInc);
                seller.addItem(item);
            }

            Auction auction = auctionService.createAuction(seller, item, price, start, end);
            
            Response broadcastRes = new Response("AUCTION_CREATED", gson.toJson(auction));
            ClientHandler.broadcast(gson.toJson(broadcastRes));

            return new Response("SUCCESS", gson.toJson(auction));
        } catch (Throwable e) {
            System.out.println("ERROR CREATE AUCTION: " + e.getMessage());
            e.printStackTrace();
            return new Response("FAIL", "create auction failed: " + e.getMessage());
        }
    }
}