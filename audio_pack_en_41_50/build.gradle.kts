plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_en_41_50")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
