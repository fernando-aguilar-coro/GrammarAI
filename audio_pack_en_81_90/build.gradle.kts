plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_en_81_90")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
