package cc.lab1;

import java.math.BigDecimal;

/**
 * Created by 1 on 29.04.2017.
 */
public class BuyMessage extends Message {
    public Long customerId;
    public BigDecimal money;

    public BuyMessage(Long customerId, BigDecimal money) {
        this.customerId = customerId;
        this.money = money;
    }
}
