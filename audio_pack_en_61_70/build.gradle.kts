plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_en_61_70")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
