plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_es_81_90")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
