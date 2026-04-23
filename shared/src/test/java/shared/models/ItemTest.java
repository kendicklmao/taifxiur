package shared.models;

import org.junit.Before;
import org.junit.Test;
import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Unit tests for Item classes
 */
public class ItemTest {
    private Seller seller;
    private Electronic electronic;

    @Before
    public void setUp() {
        seller = new Seller("seller1", "Password@123", "seller@test.com",
                "Q1", "A1", "Q2", "A2");
        electronic = new Electronic("iPhone 15", "Latest iPhone model", seller, "Apple", ItemStatus.NEW);
    }

    /**
     * Test electronic item creation
     */
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

    /**
     * Test item name getter and setter
     */
    @Test
    public void testItemNameGetterSetter() {
        electronic.setName("iPhone 16");
        assertEquals("iPhone 16", electronic.getName());
    }

    /**
     * Test item description getter and setter
     */
    @Test
    public void testItemDescriptionGetterSetter() {
        electronic.setDescription("New description");
        assertEquals("New description", electronic.getDescription());
    }

    /**
     * Test item seller getter
     */
    @Test
    public void testItemSellerGetter() {
        assertEquals(seller, electronic.getSeller());
    }

    /**
     * Test item category getter and setter
     */
    @Test
    public void testItemCategoryGetterSetter() {
        electronic.setCategory(Category.ELECTRONICS);
        assertEquals(Category.ELECTRONICS, electronic.getCategory());
    }

    /**
     * Test electronic item minimum increment
     */
    @Test
    public void testElectronicMinIncrement() {
        BigDecimal increment = electronic.getMinIncrement();
        assertEquals(0, increment.compareTo(new BigDecimal("100000")));
    }

    /**
     * Test valid item with proper name and description
     */
    @Test
    public void testValidItem() {
        assertTrue(electronic.isValid());
    }

    /**
     * Test invalid item with null name
     */
    @Test
    public void testInvalidItemNullName() {
        Electronic invalidItem = new Electronic(null, "Description", seller, "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    /**
     * Test invalid item with blank name
     */
    @Test
    public void testInvalidItemBlankName() {
        Electronic invalidItem = new Electronic("   ", "Description", seller, "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    /**
     * Test invalid item with null description
     */
    @Test
    public void testInvalidItemNullDescription() {
        Electronic invalidItem = new Electronic("iPhone", null, seller, "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    /**
     * Test invalid item with blank description
     */
    @Test
    public void testInvalidItemBlankDescription() {
        Electronic invalidItem = new Electronic("iPhone", "   ", seller, "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    /**
     * Test item with empty string name is invalid
     */
    @Test
    public void testInvalidItemEmptyName() {
        Electronic invalidItem = new Electronic("", "Description", seller, "Apple", ItemStatus.NEW);
        assertFalse(invalidItem.isValid());
    }

    /**
     * Test item electronic brand getter
     */
    @Test
    public void testElectronicBrandGetter() {
        assertEquals("Apple", electronic.getBrand());
    }

    /**
     * Test electronic item status getter
     */
    @Test
    public void testElectronicStatusGetter() {
        assertEquals(ItemStatus.NEW, electronic.getStatus());
    }

    /**
     * Test electronic item with used status
     */
    @Test
    public void testElectronicUsedStatus() {
        Electronic usedElectronic = new Electronic("Laptop", "Used laptop", seller, "Dell", ItemStatus.USED);
        assertEquals(ItemStatus.USED, usedElectronic.getStatus());
    }
}
