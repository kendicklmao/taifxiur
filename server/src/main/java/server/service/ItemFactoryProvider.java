package server.service;

import shared.enums.Category;

import java.util.*;

public class ItemFactoryProvider {

    private static final Map<Category, ItemFactory> FACTORIES =
            Map.of(
                    Category.ELECTRONICS, new ElectronicFactory(),
                    Category.VEHICLES, new VehicleFactory(),
                    Category.ARTS, new ArtFactory(),
                    Category.FASHIONS, new FashionFactory(),
                    Category.COLLECTIBLES, new CollectibleFactory()
            );

    public static ItemFactory getFactory(Category category) {
        return FACTORIES.get(category);
    }
}
