plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_en_51_60")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
