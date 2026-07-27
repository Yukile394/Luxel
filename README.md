# Luxel — Dinamik Isik Fabric Modu

Minecraft Fabric 1.21.x icin OptiFine/Shader gerektirmeyen, tamamen client-side calisan
dinamik isik modu. Elinde meşale, fener, lav kovasi vb. tasidiginda cevren gercek zamanli
aydinlanir.

## Ozellikler
- 20+ varsayilan isik kaynagi (torch, soul torch, lantern, froglight, lava bucket, mumlar...)
- Modular `ItemLightRegistry` — yeni esya eklemek tek satir kod
- Mesafe bazli culling + tick araligi kontrolu (performans modu)
- Diger oyuncular, moblar ve dusen esyalar icin ayri ayri acilir/kapanir isik destegi
- Renkli isik altyapisi (soul mavi, lava turuncu, sea lantern cyan)
- Mod Menu destekli, bagimliliksiz ayarlar ekrani
- GitHub Actions ile otomatik derleme

## Kurulum (Gelistirme)

1. Projeyi ac / klonla.
2. **Onemli:** Bu ortamda internet erisimi olmadigi icin `gradle/wrapper/gradle-wrapper.jar`
   binary dosyasini olusturamadim. Ilk calistirmadan once, sistemine kurulu bir Gradle ile
   (veya IDE'nin "Generate Gradle Wrapper" ozelligiyle) proje kokunde bir kere sunu calistir:
   ```
   gradle wrapper --gradle-version 8.10
   ```
   Bu, eksik olan `gradle-wrapper.jar` dosyasini otomatik indirip olusturacaktir.
   Bundan sonra `./gradlew build` (Linux/Mac) veya `gradlew.bat build` (Windows) normal calisir.
3. `./gradlew build` — `build/libs/luxel-1.0.0.jar` olusur.

## Isik Motoru Notu (onemli)

Dinamik isigin vanilla isik motoruna enjekte edildigi asil nokta
`WorldChunkLuminanceMixin` sinifidir (`getLuminance(BlockPos)` metodunu hedefler).
Yarn mapping surumune gore bu metodun imzasi degisebilir. Eger derleme sirasinda

```
Unable to locate method ... getLuminance ...
```

gibi bir mixin hatasi alirsan, once `WorldChunk` sinifini IDE'de decompiled halde ac
(Yarn mapping'lerine gore `int getLuminance(BlockPos pos)` benzeri bir metod ara) ve
`WorldChunkLuminanceMixin` icindeki `method = "getLuminance"` satirini gordugun gercek
imzaya gore guncelle. Bu, gecmiste EclipseHollowWatcher'da yasadigimiz refmap/mixin
hatasiyla ayni kategoriden bir sorundur — genelde tek satirlik bir duzeltme yeterli olur.
Bu adimda takilirsan bana derleme hatasinin tam metnini gonder, birlikte duzeltelim.

## Yeni Isik Kaynagi Eklemek

```java
ItemLightRegistry.register(Items.YOUR_ITEM, 12); // seviye 0-15
ItemLightRegistry.register(Items.YOUR_ITEM, 12, LightEntry.SOUL_BLUE); // renkli
```

Bu satiri `ItemLightRegistry.bootstrapDefaults()` icine eklemen yeterli.

## Eksik / Sonraki Adimlar
- `icon.png` (assets/luxel/icon.png) — henuz eklenmedi, 128x128 bir PNG ekleyip
  `fabric.mod.json`'daki yolu kullan.
- Gercek renkli isik render katmani (su an sadece veri modeli hazir, `coloredLight`
  ayari acikken renk degerleri kullanilabilir durumda ama shader/post-process
  render'a baglanmadi).
- LOD/Frustum culling su an basit mesafe kontrolu ile yapiliyor; ileri seviye
  frustum culling icin `MinecraftClient.getInstance().gameRenderer.getCamera()`
  yonu ile ek filtre eklenebilir.

## Lisans
MIT — bkz. `LICENSE`.
