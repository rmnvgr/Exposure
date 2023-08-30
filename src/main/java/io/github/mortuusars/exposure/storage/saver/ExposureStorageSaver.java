package io.github.mortuusars.exposure.storage.saver;

import io.github.mortuusars.exposure.storage.ExposureSavedData;
import io.github.mortuusars.exposure.storage.ExposureStorage;

public class ExposureStorageSaver implements IExposureSaver {
    @Override
    public void save(String id, byte[] materialColorPixels, int width, int height) {
        ExposureSavedData exposureSavedData =
                new ExposureSavedData(width, height, materialColorPixels);

        ExposureStorage.storeOnClientAndSendToServer(id, exposureSavedData);
    }
}
