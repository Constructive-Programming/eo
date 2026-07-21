# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `a4adb9054bbcb838e4b4127bd9d6f93cdbb6528c` · date: `2026-07-21` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.8 ± 0.1 | 10.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.8 ± 0.3 | 23.7 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 162.9 ± 4.8 | — | 704.0 | — |
| `ModifyCountry` | `-` | 365.9 ± 20.1 | — | 3,194.7 | — |
| `ModifyPartner` | `-` | 453.5 ± 8.6 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 174.2 ± 6.7 | — | 520.0 | — |
| `ReadPartner` | `-` | 207.5 ± 3.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 328.2 ± 6.2 | — | 1,170.7 | — |
| `naiveModifyCountry` | `-` | 2,707.2 ± 42.0 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,664.4 ± 15.7 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,124.7 ± 124.2 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,715.6 ± 16.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,721.0 ± 4.7 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 766.1 ± 8.8 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 587.5 ± 4.6 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 402.0 ± 1.0 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 423.4 ± 1.7 | — | 1,504.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,318.8 ± 11.5 | — | 3,640.0 | — |
| `freshDecodeRecord` | `-` | 1,291.1 ± 15.7 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,465.9 ± 16.3 | — | 9,416.0 | — |
| `ClickToJson` | `-` | 2,992.5 ± 26.8 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 1,043.5 ± 17.8 | — | 6,552.0 | — |
| `WideToJson` | `-` | 738.8 ± 30.7 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,757.1 ± 12.9 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,818.0 ± 61.3 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,019.4 ± 15.6 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,957.3 ± 49.1 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 226.6 ± 3.6 | — | 984.0 | — |
| `decode_native` | `-` | 18.5 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 221.6 ± 0.4 | — | 984.0 | — |
| `encode_bridged` | `-` | 270.5 ± 4.3 | — | 1,288.0 | — |
| `encode_native` | `-` | 14.7 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 253.7 ± 5.9 | — | 1,288.0 | — |
| `fieldGet_bridged` | `-` | 97.2 ± 2.1 | — | 432.0 | — |
| `fieldGet_native` | `-` | 97.6 ± 2.7 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 448.1 ± 10.1 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 177.9 ± 2.5 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.5 ± 0.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.9 ± 0.6 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.8 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.6 ± 0.4 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.9 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 41.4 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.4 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.3 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 133.1 ± 0.4 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.5 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,652.8 ± 193.3 | 4,492.7 ± 29.7 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 324.6 ± 0.5 | 307.2 ± 1.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.0 | 20.4 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,781.3 ± 10.4 | 2,769.9 ± 5.1 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 349.5 ± 0.6 | 353.5 ± 1.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.3 ± 0.1 | 44.6 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.1 ± 0.2 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.3 ± 0.3 | 26.2 ± 0.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.9 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 408,248.7 ± 2,517.3 | — | 1,073,020.7 | — |
| `cModifyId` | `size=64` | 52,529.1 ± 603.6 | — | 136,398.0 | — |
| `cModifyId` | `size=8` | 8,768.7 ± 69.0 | — | 20,824.1 | — |
| `cReadId` | `size=512` | 215,581.6 ± 1,802.8 | — | 804,181.3 | — |
| `cReadId` | `size=64` | 27,953.1 ± 382.0 | — | 101,397.8 | — |
| `cReadId` | `size=8` | 4,466.2 ± 78.4 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 220,815.8 ± 7,433.7 | — | 804,350.8 | — |
| `cReadStreet` | `size=64` | 27,252.6 ± 460.9 | — | 101,556.8 | — |
| `cReadStreet` | `size=8` | 4,464.9 ± 17.8 | — | 15,832.0 | — |
| `cReplaceId` | `size=512` | 407,933.4 ± 8,658.7 | — | 1,072,950.0 | — |
| `cReplaceId` | `size=64` | 52,931.1 ± 972.4 | — | 136,340.4 | — |
| `cReplaceId` | `size=8` | 8,779.8 ± 50.3 | — | 20,744.1 | — |
| `cSumPrices` | `size=512` | 347,051.0 ± 2,649.4 | — | 1,253,090.7 | — |
| `cSumPrices` | `size=64` | 43,286.8 ± 232.3 | — | 157,833.7 | — |
| `cSumPrices` | `size=8` | 6,441.0 ± 75.0 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 164.1 ± 1.1 | — | 0.0 | — |
| `jMiss` | `size=64` | 164.1 ± 0.3 | — | 0.0 | — |
| `jMiss` | `size=8` | 164.0 ± 0.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,211.4 ± 40.5 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 410.7 ± 4.1 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 106.1 ± 0.9 | — | 992.0 | — |
| `jReadId` | `size=512` | 36.2 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=64` | 39.4 ± 0.4 | — | 72.0 | — |
| `jReadId` | `size=8` | 36.2 ± 2.3 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 178.9 ± 1.1 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 187.5 ± 14.7 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 179.4 ± 1.2 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,210.2 ± 34.3 | — | 41,888.9 | — |
| `jReplaceId` | `size=64` | 405.6 ± 4.5 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 101.7 ± 1.1 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 85,580.6 ± 4,546.5 | — | 63,665.9 | — |
| `jSumPrices` | `size=64` | 10,492.1 ± 281.4 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,415.3 ± 27.7 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.6 ± 0.2 | 31.5 ± 0.4 | 152.0 | 176.0 |
| `Replace` | `-` | 3.2 ± 0.0 | 3.0 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,429.4 ± 556.6 | — | 380,269.0 | — |
| `Modify_multiFocus` | `size=256` | 13,082.8 ± 361.3 | — | 88,422.2 | — |
| `Modify_multiFocus` | `size=32` | 1,538.7 ± 16.6 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 245.9 ± 3.1 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,688.4 ± 203.9 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,766.5 ± 127.5 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,104.2 ± 12.6 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 207.2 ± 12.6 | — | 920.0 | — |
| `naive_listMap` | `size=1024` | 8,307.7 ± 53.2 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,048.4 ± 6.6 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 246.1 ± 1.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.9 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 67.0 ± 0.6 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 164.6 ± 0.6 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.7 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.3 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.2 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.4 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.4 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.7 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,243.8 ± 21.5 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,150.7 ± 16.2 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.9 ± 0.1 | 22.5 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.7 ± 0.6 | 70.6 ± 0.3 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.4 ± 0.7 | 116.9 ± 1.5 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.7 ± 0.1 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.9 ± 0.0 | 6.8 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,103.6 ± 139.2 | — | 97,656.8 | — |
| `ModifyNames` | `size=64` | 4,760.9 ± 70.3 | — | 12,792.2 | — |
| `ModifyNames` | `size=8` | 659.2 ± 1.8 | — | 2,192.0 | — |
| `ModifyStreet` | `size=512` | 134.0 ± 1.5 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 135.0 ± 2.5 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 132.8 ± 0.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 40.7 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.8 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 41.3 ± 1.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 103,211.8 ± 971.9 | — | 382,772.1 | — |
| `monocleModifyNames` | `size=64` | 9,980.9 ± 112.6 | — | 39,834.4 | — |
| `monocleModifyNames` | `size=8` | 1,573.2 ± 64.9 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 55,699.7 ± 1,114.5 | — | 169,084.9 | — |
| `monocleModifyStreet` | `size=64` | 7,008.5 ± 555.8 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 1,027.3 ± 6.4 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,929.6 ± 312.0 | — | 69,789.4 | — |
| `monocleReadStreet` | `size=64` | 4,191.8 ± 29.3 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 512.0 ± 6.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,071.9 ± 264.0 | — | 226,301.2 | — |
| `naiveModifyNames` | `size=64` | 8,923.2 ± 20.1 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,227.1 ± 8.3 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,056.5 ± 485.7 | — | 169,061.7 | — |
| `naiveModifyStreet` | `size=64` | 7,142.1 ± 23.7 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 1,023.7 ± 24.9 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,856.5 ± 167.3 | — | 69,789.1 | — |
| `naiveReadStreet` | `size=64` | 4,191.2 ± 35.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 509.0 ± 3.9 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 273,262.3 ± 5,286.3 | — | 650,975.1 | — |
| `Names` | `size=64` | 34,364.5 ± 325.4 | — | 82,939.5 | — |
| `Names` | `size=8` | 4,852.2 ± 124.1 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 299,240.1 ± 3,181.4 | — | 724,611.6 | — |
| `NamesIor` | `size=64` | 38,449.9 ± 1,367.6 | — | 90,649.8 | — |
| `NamesIor` | `size=8` | 5,040.4 ± 42.2 | — | 12,021.4 | — |
| `Street` | `size=512` | 1,160.6 ± 12.8 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,168.1 ± 18.6 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,150.6 ± 16.1 | — | 2,960.0 | — |
| `StreetIor` | `size=512` | 1,163.0 ± 9.4 | — | 2,984.9 | — |
| `StreetIor` | `size=64` | 1,166.5 ± 9.0 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,162.0 ± 29.3 | — | 2,984.0 | — |
| `directNames` | `size=512` | 257,707.9 ± 10,555.4 | — | 609,914.9 | — |
| `directNames` | `size=64` | 32,144.8 ± 420.8 | — | 77,712.0 | — |
| `directNames` | `size=8` | 4,518.2 ± 143.9 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 1,118.5 ± 31.8 | — | 2,744.9 | — |
| `directStreet` | `size=64` | 1,092.9 ± 18.6 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,092.6 ± 12.7 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 262,563.5 ± 3,411.4 | — | 609,929.4 | — |
| `hcursorNames` | `size=64` | 32,094.9 ± 928.9 | — | 77,791.5 | — |
| `hcursorNames` | `size=8` | 4,445.9 ± 121.4 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,189.6 ± 15.7 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,161.8 ± 21.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,192.4 ± 19.5 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 239,506.0 ± 2,221.2 | — | 1,121,768.9 | — |
| `monocleNames` | `size=64` | 25,942.4 ± 159.1 | — | 132,786.2 | — |
| `monocleNames` | `size=8` | 4,057.0 ± 122.0 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 192,067.2 ± 2,713.4 | — | 908,033.0 | — |
| `monocleStreet` | `size=64` | 22,713.8 ± 244.3 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,445.3 ± 22.1 | — | 17,074.7 | — |
| `naiveNames` | `size=512` | 205,084.4 ± 1,471.6 | — | 965,273.8 | — |
| `naiveNames` | `size=64` | 24,561.2 ± 244.8 | — | 120,832.1 | — |
| `naiveNames` | `size=8` | 3,630.7 ± 27.0 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 192,315.0 ± 2,508.5 | — | 908,038.5 | — |
| `naiveStreet` | `size=64` | 23,938.3 ± 1,953.0 | — | 113,781.2 | — |
| `naiveStreet` | `size=8` | 3,456.7 ± 31.0 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,678.1 ± 279.7 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 581.5 ± 8.6 | — | 5,440.1 | — |
| `ModifyStreet` | `size=8` | 290.9 ± 15.4 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 183.3 ± 2.2 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 179.9 ± 1.2 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 186.5 ± 12.4 | — | 128.0 | — |
| `SumPrices` | `size=512` | 83,970.8 ± 3,207.3 | — | 63,718.2 | — |
| `SumPrices` | `size=64` | 10,292.4 ± 71.2 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,444.2 ± 42.3 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 167,761.4 ± 1,100.8 | — | 333,612.1 | — |
| `monocleModifyStreet` | `size=64` | 20,499.0 ± 151.5 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,392.0 ± 25.3 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,254.8 ± 737.4 | — | 193,265.5 | — |
| `monocleReadStreet` | `size=64` | 12,162.4 ± 124.6 | — | 24,737.2 | — |
| `monocleReadStreet` | `size=8` | 1,925.9 ± 11.8 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 446,607.7 ± 1,664.7 | — | 1,190,798.8 | — |
| `monocleSumPrices` | `size=64` | 16,745.0 ± 126.8 | — | 47,425.7 | — |
| `monocleSumPrices` | `size=8` | 2,641.6 ± 7.6 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 167,861.5 ± 3,245.0 | — | 333,568.9 | — |
| `naiveModifyStreet` | `size=64` | 20,478.5 ± 87.3 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,426.8 ± 21.5 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 97,329.8 ± 2,807.1 | — | 193,267.3 | — |
| `naiveReadStreet` | `size=64` | 12,232.1 ± 319.9 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,923.0 ± 7.1 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 100,302.0 ± 720.0 | — | 230,157.9 | — |
| `naiveSumPrices` | `size=64` | 12,729.2 ± 44.4 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,013.9 ± 11.3 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 38,620.7 ± 1,859.0 | — | 456.3 | — |
| `nativeReadStreet` | `size=64` | 4,765.5 ± 29.5 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 808.1 ± 7.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 64,462.8 ± 319.8 | — | 86,268.0 | — |
| `nativeSumPrices` | `size=64` | 8,181.2 ± 144.1 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,192.8 ± 9.8 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,671.2 ± 4,003.1 | — | 624,393.3 | — |
| `TransformDeep` | `n=512` | 12,971.9 ± 75.7 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,612.4 ± 23.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 146,908.8 ± 653.4 | 177,230.2 ± 887.3 | 655,370.9 | 753,745.0 |
| `TransformExpr` | `n=512` | 18,222.2 ± 96.2 | 15,961.6 ± 106.6 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,253.8 ± 7.2 | 2,718.0 ± 11.8 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 100,642.5 ± 4,590.9 | — | 786,577.2 | — |
| `UniverseDeep` | `n=512` | 15,438.6 ± 139.3 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,874.0 ± 10.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 98,012.3 ± 1,273.0 | 1,733,828.5 ± 87,569.1 | 786,383.3 | 4,752,476.7 |
| `UniverseExpr` | `n=512` | 14,826.8 ± 449.3 | 170,485.0 ± 652.8 | 98,185.5 | 483,201.4 |
| `UniverseExpr` | `n=64` | 1,758.9 ± 10.4 | 16,629.6 ± 773.9 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 181,571.4 ± 1,104.4 | 2,001,358.7 ± 78,116.8 | 786,444.1 | 6,488,999.1 |
| `UniverseJson` | `n=512` | 20,380.5 ± 81.1 | 211,520.8 ± 2,077.8 | 98,186.1 | 699,917.6 |
| `UniverseJson` | `n=64` | 2,471.5 ± 13.3 | 21,340.9 ± 127.6 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,928.0 ± 181.7 | — | 163,881.4 | — |
| `visitorTransformDeep` | `n=512` | 4,150.8 ± 55.2 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 432.9 ± 2.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 66,878.7 ± 283.4 | — | 360,472.7 | — |
| `visitorTransformExpr` | `n=512` | 8,341.3 ± 50.5 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,035.6 ± 11.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,914.1 ± 983.5 | — | 196,705.4 | — |
| `visitorUniverseDeep` | `n=512` | 6,836.0 ± 49.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 803.7 ± 6.3 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,306.8 ± 454.9 | — | 196,656.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,838.2 ± 33.9 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 837.1 ± 6.8 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 150,883.0 ± 21,233.1 | — | 313,725.8 | — |
| `visitorUniverseJson` | `n=512` | 13,931.4 ± 34.7 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,305.0 ± 241.6 | — | 5,264.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,885.5 ± 126.5 | — | 41,438.6 | — |
| `Modify_powerEach` | `size=16` | 297.4 ± 4.3 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,331.7 ± 25.7 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 138.0 ± 1.8 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 56,996.0 ± 639.9 | — | 164,432.0 | — |
| `Modify_powerEach` | `size=64` | 911.4 ± 17.8 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 60,645.2 ± 1,493.9 | — | 279,434.8 | — |
| `monocle_powerEach` | `size=16` | 605.8 ± 29.7 | — | 3,725.3 | — |
| `monocle_powerEach` | `size=256` | 22,043.0 ± 451.5 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 257.6 ± 3.8 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 188,578.6 ± 7,687.1 | — | 967,877.6 | — |
| `monocle_powerEach` | `size=64` | 2,096.9 ± 29.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,525.2 ± 18.8 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.9 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,611.1 ± 2.9 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.4 ± 0.8 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,409.9 ± 462.7 | — | 114,782.7 | — |
| `naive_powerEach` | `size=64` | 395.0 ± 1.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 65,659.6 ± 2,588.9 | — | 210,976.9 | — |
| `Modify_nested` | `size=16` | 1,678.7 ± 8.7 | — | 5,269.4 | — |
| `Modify_nested` | `size=256` | 16,672.6 ± 573.3 | — | 54,210.5 | — |
| `Modify_nested` | `size=4` | 821.1 ± 8.9 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,204.8 ± 176.7 | — | 15,043.2 | — |
| `monocle_nested` | `size=1024` | 258,365.4 ± 1,766.4 | — | 1,118,895.8 | — |
| `monocle_nested` | `size=16` | 2,891.1 ± 21.2 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 96,319.9 ± 2,890.2 | — | 430,213.8 | — |
| `monocle_nested` | `size=4` | 1,364.2 ± 122.0 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,988.3 ± 175.5 | — | 58,902.4 | — |
| `naive_nested` | `size=1024` | 21,599.8 ± 930.2 | — | 115,073.5 | — |
| `naive_nested` | `size=16` | 389.4 ± 7.6 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,810.7 ± 25.5 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 134.1 ± 2.9 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,413.6 ± 14.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,545.1 ± 16.5 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 28,577.7 ± 328.0 | — | 104,800.7 | — |
| `Modify_sparse` | `size=32` | 512.3 ± 2.9 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,106.3 ± 89.8 | — | 24,906.0 | — |
| `Modify_sparse` | `size=8` | 175.0 ± 1.2 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,711.5 ± 34.5 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 100,766.9 ± 1,425.8 | — | 476,033.7 | — |
| `monocle_sparse` | `size=32` | 1,013.1 ± 23.7 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 33,774.1 ± 2,737.0 | — | 156,444.4 | — |
| `monocle_sparse` | `size=8` | 311.8 ± 3.7 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 325.9 ± 1.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,410.5 ± 39.6 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,290.5 ± 6.3 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.1 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.1 | 2.4 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.7 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 142,759.3 ± 1,180.4 | — | 786,297.0 | — |
| `Cata` | `-` | 84,353.1 ± 246.0 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,165.2 ± 302.6 | — | 295,848.6 | — |
| `drosteAna` | `-` | 56,986.9 ± 427.8 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,776.2 ± 262.2 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,497.2 ± 301.4 | — | 328,640.5 | — |
| `handAna` | `-` | 20,708.7 ± 2,137.4 | — | 163,816.1 | — |
| `handCata` | `-` | 13,183.8 ± 183.0 | — | 0.1 | — |
| `handHylo` | `-` | 11,473.5 ± 218.7 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.5 ± 0.0 | 25.9 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 25.9 ± 0.1 | 59.4 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.0 ± 0.0 | 3.0 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,054.4 ± 38.7 | 35,719.7 ± 229.3 | 39,001.1 | 176,924.5 |
| `Modify` | `size=64` | 957.0 ± 2.8 | 1,758.2 ± 12.9 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 109.5 ± 0.4 | 290.0 ± 4.6 | 728.0 | 1,936.0 |

