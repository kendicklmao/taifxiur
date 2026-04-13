package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import shared.models.Auction;
import shared.models.Bidder;
import shared.models.User;

import java.math.BigDecimal;

public class AuctionDetailController {
    @FXML
    Label itemName;
    @FXML
    Label currentPrice;
    @FXML
    TextField bidAmount;

    private final AppContext ctx = AppContext.getInstance();
    private Auction auction;

    @FXML
    public void initialize() {
        auction = ctx.getSelectedAuction();
        itemName.setText(auction.getItem().getName());
        updatePrice();
    }

    private void updatePrice() {
        currentPrice.setText(auction.getCurrentPrice().toString());
    }

    @FXML
    public void placeBid() {
        /*try{
            BigDecimal amount = new BigDecimal(bidAmount.getText());
            User u = ctx.getCurrentUser();

            if(u instanceof Bidder bidder){
                boolean ok = ctx.getAuctionService()
                    .placeBid(auction.getId(), bidder, amount);
                if(ok) updatePrice();
            }
        }catch(Exception e){ e.printStackTrace(); }*/
    }
}
