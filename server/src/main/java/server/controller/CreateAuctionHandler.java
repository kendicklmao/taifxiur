package server.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.Gson;
import server.service.*;
import shared.enums.Category;
import shared.items.ItemFactory;
import shared.items.ItemFactoryProvider;
import shared.utils.GsonUtils;

import shared.models.Auction;
import shared.models.Item;
import shared.models.users.Seller;
import shared.models.users.User;
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

            if (userService.isBanned(username)) {
                return new Response(
                        "FAIL",
                        "Your account has been banned. You cannot perform this action.");
            }

            User user = userService.getUser(username);

            if (!(user instanceof Seller seller)) {
                return new Response("FAIL", "Invalid user");
            }
            
            BigDecimal price = new BigDecimal(data.get("price"))
                    .setScale(2, RoundingMode.UP);

            Instant start = Instant.parse(data.get("startTime"));
            Instant end = Instant.parse(data.get("endTime"));

            String imageUrl = null;
            String imageBase64 = data.get("image");

            if (imageBase64 != null && !imageBase64.isEmpty()) {
                byte[] imageBytes =
                        Base64.getDecoder().decode(imageBase64);

                String imageId = UUID.randomUUID().toString();

                imageUrl = storageService.uploadFile(
                        imageId,
                        imageBytes,
                        data.get("imageContentType"));
            }

            Category category =
                    Category.valueOf(data.get("category"));

            ItemFactory factory =
                    ItemFactoryProvider.getFactory(category);

            if (factory == null) {
                return new Response(
                        "FAIL",
                        "Unsupported category: " + category);
            }

            Item item = factory.create(
                    data,
                    seller,
                    price);

            item.setImageUrl(imageUrl);

            BigDecimal defaultMinInc = price
                    .multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.UP);

            BigDecimal minInc;

            if ("Custom Amount".equals(data.get("incrementType"))) {

                minInc = new BigDecimal(data.get("minIncrement"))
                        .setScale(2, RoundingMode.UP);

                if (minInc.compareTo(defaultMinInc) < 0) {
                    return new Response(
                            "FAIL",
                            "Custom increment must be greater than or equal to default minimum increment: "
                                    + defaultMinInc.toPlainString());
                }

            } else {
                minInc = defaultMinInc;
            }

            item.setMinIncrement(minInc);

            seller.addItem(item);

            Auction auction = auctionService.createAuction(
                    seller,
                    item,
                    price,
                    start,
                    end);

            Response broadcastRes =
                    new Response(
                            "AUCTION_CREATED",
                            gson.toJson(auction));

            ClientHandler.broadcast(
                    gson.toJson(broadcastRes));

            return new Response(
                    "SUCCESS",
                    gson.toJson(auction));

        } catch (Throwable e) {
            System.out.println(
                    "ERROR CREATE AUCTION: " + e.getMessage());

            e.printStackTrace();

            return new Response(
                    "FAIL",
                    "create auction failed: " + e.getMessage());
        }
    }
}