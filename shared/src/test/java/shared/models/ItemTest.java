package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.models.users.Seller;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {
    private Seller seller;
    private Electronic electronic;

    @BeforeEach
    public void setUp() {
        seller = new Seller("seller1", "Password@123", "seller@test.com", "Q1", "A1", "Q2", "A2");
        electronic = new Electronic("iPhone 15", "Latest iPhone model", seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        electronic.setMinIncrement(new BigDecimal("100000"));
    }

    @Test
    public void testElectronicItemCreation() {
        assertNotNull(electronic);
        assertEquals("iPhone 15", electronic.getName());
        assertEquals("Latest iPhone model", electronic.getDescription());
        assertEquals(seller, electronic.getSeller());
        assertEquals("Apple", electronic.getBrand());
        assertEquals(ItemStatus.NEW, electronic.getStatus());
        assertEquals(Category.ELECTRONICS, electronic.getCategory());
    }

    @Test
    public void testItemNameGetterSetter() {
        electronic.setName("iPhone 16");
        assertEquals("iPhone 16", electronic.getName());
    }

    @Test
    public void testItemDescriptionGetterSetter() {
        electronic.setDescription("New description");
        assertEquals("New description", electronic.getDescription());
    }

    @Test
    public void testItemSellerGetter() {
        assertEquals(seller, electronic.getSeller());
    }

    @Test
    public void testItemCategoryGetterSetter() {
        electronic.setCategory(Category.ELECTRONICS);
        assertEquals(Category.ELECTRONICS, electronic.getCategory());
    }

    @Test
    public void testElectronicMinIncrement() {
        BigDecimal increment = electronic.getMinIncrement();
        assertEquals(0, increment.compareTo(new BigDecimal("100000")));
    }

    @Test
    public void testValidItem() {
        assertTrue(electronic.isValid());
    }
    @Test
    public void testInvalidItemNullName() {
        Electronic invalidItem = new Electronic(null, "Description", seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    @Test
    public void testInvalidItemBlankName() {
        Electronic invalidItem = new Electronic("   ", "Description", seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    @Test
    public void testInvalidItemNullDescription() {
        Electronic invalidItem = new Electronic("iPhone", null, seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    @Test
    public void testInvalidItemBlankDescription() {
        Electronic invalidItem = new Electronic("iPhone", "   ", seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    @Test
    public void testInvalidItemEmptyName() {
        Electronic invalidItem = new Electronic("", "Description", seller, new BigDecimal("1000"), "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    @Test
    public void testElectronicBrandGetter() {
        assertEquals("Apple", electronic.getBrand());
    }
    @Test
    public void testElectronicStatusGetter() {
        assertEquals(ItemStatus.NEW, electronic.getStatus());
    }

    @Test
    public void testElectronicUsedStatus() {
        Electronic usedElectronic = new Electronic("Laptop", "Used laptop", seller, new BigDecimal("1000"), "Dell", ItemStatus.USED);
        assertEquals(ItemStatus.USED, usedElectronic.getStatus());
    }
}