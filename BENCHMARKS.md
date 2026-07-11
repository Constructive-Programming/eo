# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `8e822e2c1281209e2cbed59a5e911d6377ba3b11` · date: `2026-07-11` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.1 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.1 | 10.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.8 ± 0.3 | 23.0 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 162.7 ± 1.4 | — | 704.0 | — |
| `ModifyCountry` | `-` | 582.5 ± 91.0 | — | 3,458.7 | — |
| `ModifyPartner` | `-` | 636.0 ± 74.6 | — | 3,504.0 | — |
| `ReadCountry` | `-` | 186.5 ± 4.3 | — | 784.0 | — |
| `ReadPartner` | `-` | 220.5 ± 6.1 | — | 744.0 | — |
| `SliceGraftPayload` | `-` | 323.0 ± 4.4 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,719.6 ± 50.1 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,729.8 ± 79.9 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,360.3 ± 87.9 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,718.6 ± 26.2 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,730.4 ± 12.0 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 763.1 ± 3.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 599.9 ± 21.6 | — | 1,592.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,890.1 ± 36.4 | — | 10,776.0 | — |
| `ClickToJson` | `-` | 3,472.7 ± 34.9 | — | 5,472.0 | — |
| `WideToAvro` | `-` | 1,093.2 ± 65.1 | — | 7,144.0 | — |
| `WideToJson` | `-` | 688.6 ± 17.5 | — | 2,016.0 | — |
| `naiveClickToAvro` | `-` | 1,759.8 ± 6.2 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,815.1 ± 47.6 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,132.7 ± 109.6 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,917.7 ± 13.1 | — | 4,376.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.4 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.3 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 33.6 ± 1.0 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.7 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.5 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 6.6 ± 0.3 | — | 72.0 | — |
| `buildLens3` | `-` | 21.7 ± 0.4 | — | 184.0 | — |
| `buildLens6` | `-` | 46.3 ± 1.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 24.0 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.2 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.8 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.3 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 133.9 ± 1.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 69.5 ± 12.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,530.8 ± 161.6 | 4,527.2 ± 53.5 | 14,080.7 | 14,080.7 |
| `FoldMap` | `size=64` | 325.7 ± 0.7 | 308.7 ± 1.0 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.1 | 20.4 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,372.3 ± 271.5 | 3,078.3 ± 208.7 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 352.4 ± 3.2 | 362.2 ± 6.1 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.3 ± 0.4 | 45.9 ± 0.5 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.1 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.1 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.9 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.6 ± 0.2 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.1 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.6 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.0 ± 0.3 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.0 ± 0.1 | 24.6 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 411,889.3 ± 2,884.6 | — | 1,073,015.3 | — |
| `cModifyId` | `size=64` | 53,583.4 ± 825.1 | — | 136,414.3 | — |
| `cModifyId` | `size=8` | 8,961.1 ± 85.0 | — | 20,800.1 | — |
| `cReadId` | `size=512` | 220,062.7 ± 1,856.9 | — | 804,182.6 | — |
| `cReadId` | `size=64` | 28,210.7 ± 634.0 | — | 101,397.4 | — |
| `cReadId` | `size=8` | 4,515.8 ± 87.5 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 218,886.7 ± 5,446.7 | — | 804,350.2 | — |
| `cReadStreet` | `size=64` | 28,035.9 ± 464.6 | — | 101,557.6 | — |
| `cReadStreet` | `size=8` | 4,499.2 ± 115.8 | — | 15,832.0 | — |
| `cReplaceId` | `size=512` | 413,804.2 ± 5,324.4 | — | 1,072,960.5 | — |
| `cReplaceId` | `size=64` | 53,521.2 ± 659.6 | — | 136,318.0 | — |
| `cReplaceId` | `size=8` | 8,835.5 ± 127.7 | — | 20,760.1 | — |
| `cSumPrices` | `size=512` | 351,760.6 ± 5,323.6 | — | 1,253,084.0 | — |
| `cSumPrices` | `size=64` | 43,579.9 ± 692.9 | — | 157,831.6 | — |
| `cSumPrices` | `size=8` | 6,600.0 ± 47.8 | — | 22,920.1 | — |
| `jMiss` | `size=512` | 167.6 ± 4.9 | — | 0.0 | — |
| `jMiss` | `size=64` | 164.2 ± 1.0 | — | 0.0 | — |
| `jMiss` | `size=8` | 164.0 ± 0.3 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,397.0 ± 111.9 | — | 41,921.0 | — |
| `jModifyId` | `size=64` | 424.4 ± 2.9 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 107.3 ± 4.3 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.1 ± 1.7 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.9 ± 1.6 | — | 56.0 | — |
| `jReadId` | `size=8` | 35.7 ± 0.4 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 178.6 ± 0.5 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 196.1 ± 11.8 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 178.5 ± 0.6 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,458.1 ± 97.2 | — | 41,889.0 | — |
| `jReplaceId` | `size=64` | 420.4 ± 6.6 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 98.5 ± 2.5 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 86,697.1 ± 1,026.1 | — | 63,665.5 | — |
| `jSumPrices` | `size=64` | 10,452.3 ± 230.7 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,410.3 ± 14.2 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.1 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 35.7 ± 1.3 | 31.4 ± 0.3 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.1 | 3.1 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,127.9 ± 499.5 | — | 380,250.4 | — |
| `Modify_multiFocus` | `size=256` | 13,071.1 ± 312.6 | — | 88,443.6 | — |
| `Modify_multiFocus` | `size=32` | 1,567.1 ± 10.8 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 251.8 ± 3.9 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,788.2 ± 280.9 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,939.9 ± 219.4 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,021.5 ± 130.1 | — | 3,272.0 | — |
| `Modify_powerEach` | `size=4` | 217.8 ± 2.2 | — | 904.0 | — |
| `naive_listMap` | `size=1024` | 8,430.5 ± 272.6 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,062.2 ± 9.7 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 247.9 ± 2.0 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.9 ± 0.1 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.9 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 165.6 ± 1.8 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 27.4 ± 0.9 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.2 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.3 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.7 ± 0.3 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.1 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.9 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,274.9 ± 52.3 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,200.6 ± 14.7 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.6 ± 0.6 | 22.3 ± 0.0 | 104.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.6 ± 0.3 | 70.9 ± 0.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.4 ± 0.7 | 114.8 ± 1.1 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.9 ± 0.4 | 20.7 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.0 ± 0.0 | 6.9 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,707.2 ± 440.5 | — | 97,656.8 | — |
| `ModifyNames` | `size=64` | 4,734.2 ± 43.1 | — | 12,800.2 | — |
| `ModifyNames` | `size=8` | 667.2 ± 5.1 | — | 2,200.0 | — |
| `ModifyStreet` | `size=512` | 135.1 ± 1.6 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 135.5 ± 4.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 133.6 ± 1.0 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 41.1 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.8 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 42.4 ± 2.6 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 101,007.8 ± 3,542.1 | — | 382,765.1 | — |
| `monocleModifyNames` | `size=64` | 10,326.4 ± 371.0 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,571.7 ± 60.3 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 55,614.2 ± 1,079.7 | — | 169,082.3 | — |
| `monocleModifyStreet` | `size=64` | 7,423.1 ± 231.2 | — | 20,896.3 | — |
| `monocleModifyStreet` | `size=8` | 1,036.4 ± 9.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,946.2 ± 287.1 | — | 69,789.8 | — |
| `monocleReadStreet` | `size=64` | 4,179.7 ± 19.8 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 522.5 ± 10.1 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,465.0 ± 1,298.4 | — | 226,301.4 | — |
| `naiveModifyNames` | `size=64` | 8,629.3 ± 415.0 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,215.6 ± 20.3 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,137.8 ± 304.6 | — | 169,061.7 | — |
| `naiveModifyStreet` | `size=64` | 6,952.1 ± 431.7 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,029.5 ± 27.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,843.4 ± 208.6 | — | 69,789.1 | — |
| `naiveReadStreet` | `size=64` | 4,210.4 ± 10.5 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 514.3 ± 5.8 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 276,426.0 ± 4,706.6 | — | 646,893.5 | — |
| `Names` | `size=64` | 34,541.4 ± 825.9 | — | 82,971.3 | — |
| `Names` | `size=8` | 4,780.3 ± 145.1 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 311,844.3 ± 6,791.6 | — | 724,625.9 | — |
| `NamesIor` | `size=64` | 37,918.1 ± 330.4 | — | 90,130.1 | — |
| `NamesIor` | `size=8` | 5,073.6 ± 82.0 | — | 11,960.1 | — |
| `Street` | `size=512` | 1,187.3 ± 16.0 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,197.4 ± 20.0 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,167.8 ± 7.7 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,171.6 ± 26.2 | — | 2,984.9 | — |
| `StreetIor` | `size=64` | 1,208.3 ± 3.5 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,177.2 ± 18.1 | — | 2,984.0 | — |
| `directNames` | `size=512` | 266,912.6 ± 9,244.8 | — | 614,013.9 | — |
| `directNames` | `size=64` | 33,139.5 ± 641.7 | — | 77,743.2 | — |
| `directNames` | `size=8` | 4,492.4 ± 101.3 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 1,117.9 ± 16.7 | — | 2,736.9 | — |
| `directStreet` | `size=64` | 1,098.1 ± 10.4 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,092.7 ± 13.8 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 260,513.4 ± 3,073.8 | — | 605,800.8 | — |
| `hcursorNames` | `size=64` | 32,971.4 ± 781.4 | — | 77,792.7 | — |
| `hcursorNames` | `size=8` | 4,522.0 ± 20.5 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,195.7 ± 27.5 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,208.3 ± 18.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,185.4 ± 9.1 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 237,983.0 ± 3,419.7 | — | 1,121,767.9 | — |
| `monocleNames` | `size=64` | 25,958.4 ± 271.4 | — | 132,786.6 | — |
| `monocleNames` | `size=8` | 4,055.8 ± 47.0 | — | 19,509.4 | — |
| `monocleStreet` | `size=512` | 192,614.6 ± 3,334.0 | — | 908,052.1 | — |
| `monocleStreet` | `size=64` | 22,794.7 ± 197.3 | — | 113,810.0 | — |
| `monocleStreet` | `size=8` | 3,455.1 ± 6.6 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 203,747.9 ± 1,000.5 | — | 965,272.9 | — |
| `naiveNames` | `size=64` | 24,362.3 ± 58.3 | — | 120,831.7 | — |
| `naiveNames` | `size=8` | 3,673.7 ± 127.8 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 188,558.1 ± 1,054.0 | — | 908,030.7 | — |
| `naiveStreet` | `size=64` | 22,869.3 ± 197.4 | — | 113,791.4 | — |
| `naiveStreet` | `size=8` | 3,450.8 ± 34.9 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,721.8 ± 30.6 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 600.6 ± 11.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 284.8 ± 8.1 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 183.4 ± 3.5 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 179.4 ± 0.7 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 180.7 ± 2.7 | — | 128.0 | — |
| `SumPrices` | `size=512` | 85,737.7 ± 3,483.2 | — | 63,719.6 | — |
| `SumPrices` | `size=64` | 10,406.4 ± 116.9 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,428.4 ± 23.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 166,004.1 ± 355.3 | — | 333,576.0 | — |
| `monocleModifyStreet` | `size=64` | 20,511.3 ± 100.4 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,551.8 ± 234.9 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,335.1 ± 777.1 | — | 193,265.6 | — |
| `monocleReadStreet` | `size=64` | 12,263.2 ± 138.2 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,923.5 ± 6.1 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 452,702.6 ± 4,500.6 | — | 1,190,803.7 | — |
| `monocleSumPrices` | `size=64` | 17,856.0 ± 2,481.2 | — | 46,900.5 | — |
| `monocleSumPrices` | `size=8` | 2,626.9 ± 59.4 | — | 6,664.1 | — |
| `naiveModifyStreet` | `size=512` | 166,001.4 ± 419.0 | — | 333,602.7 | — |
| `naiveModifyStreet` | `size=64` | 20,642.7 ± 328.9 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,438.6 ± 53.8 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 96,930.2 ± 2,136.4 | — | 193,267.0 | — |
| `naiveReadStreet` | `size=64` | 12,231.6 ± 159.3 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 2,172.4 ± 336.9 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 100,881.3 ± 1,161.3 | — | 230,158.3 | — |
| `naiveSumPrices` | `size=64` | 12,811.0 ± 123.0 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,013.8 ± 7.0 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 38,535.3 ± 1,693.3 | — | 455.7 | — |
| `nativeReadStreet` | `size=64` | 4,763.5 ± 21.9 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 804.9 ± 4.0 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,542.5 ± 1,103.6 | — | 86,269.4 | — |
| `nativeSumPrices` | `size=64` | 8,018.9 ± 162.2 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,200.3 ± 13.2 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,169.6 ± 2,210.5 | — | 624,392.9 | — |
| `TransformDeep` | `n=512` | 13,054.8 ± 218.8 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,606.7 ± 11.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 149,432.0 ± 2,874.7 | 178,274.3 ± 3,250.4 | 655,372.8 | 753,745.7 |
| `TransformExpr` | `n=512` | 18,265.3 ± 75.5 | 16,094.8 ± 151.2 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,261.8 ± 6.2 | 2,777.6 ± 96.3 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 100,887.2 ± 1,520.9 | — | 786,577.4 | — |
| `UniverseDeep` | `n=512` | 15,392.2 ± 57.2 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,873.5 ± 15.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 98,326.0 ± 1,006.6 | 1,742,719.9 ± 61,109.1 | 786,383.6 | 4,796,168.2 |
| `UniverseExpr` | `n=512` | 14,618.7 ± 174.6 | 167,357.7 ± 1,669.0 | 98,185.5 | 483,201.2 |
| `UniverseExpr` | `n=64` | 1,761.6 ± 13.9 | 17,164.9 ± 730.2 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 180,828.1 ± 1,323.6 | 1,926,032.5 ± 7,692.8 | 786,443.6 | 6,488,944.3 |
| `UniverseJson` | `n=512` | 20,370.0 ± 189.3 | 212,176.6 ± 2,732.4 | 98,186.1 | 699,917.6 |
| `UniverseJson` | `n=64` | 2,469.2 ± 8.0 | 20,992.0 ± 281.1 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,932.5 ± 257.8 | — | 163,881.4 | — |
| `visitorTransformDeep` | `n=512` | 4,108.5 ± 22.2 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 432.9 ± 2.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,465.4 ± 709.1 | — | 360,473.1 | — |
| `visitorTransformExpr` | `n=512` | 8,375.1 ± 22.9 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,037.6 ± 7.8 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,223.0 ± 519.8 | — | 196,704.9 | — |
| `visitorUniverseDeep` | `n=512` | 6,844.7 ± 51.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 804.3 ± 4.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,163.1 ± 474.3 | — | 196,656.1 | — |
| `visitorUniverseExpr` | `n=512` | 6,804.8 ± 15.4 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 1,029.0 ± 298.5 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 164,873.7 ± 18,427.7 | — | 332,456.0 | — |
| `visitorUniverseJson` | `n=512` | 14,513.8 ± 1,030.6 | — | 38,209.5 | — |
| `visitorUniverseJson` | `n=64` | 2,279.2 ± 259.6 | — | 5,264.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,907.5 ± 79.1 | — | 41,438.5 | — |
| `Modify_powerEach` | `size=16` | 300.3 ± 7.1 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,334.7 ± 65.1 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 136.6 ± 0.5 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 56,845.9 ± 306.7 | — | 164,431.8 | — |
| `Modify_powerEach` | `size=64` | 901.7 ± 1.7 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 60,137.8 ± 1,929.2 | — | 279,434.4 | — |
| `monocle_powerEach` | `size=16` | 641.2 ± 11.2 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 22,576.0 ± 357.2 | — | 107,331.6 | — |
| `monocle_powerEach` | `size=4` | 247.5 ± 16.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 182,542.6 ± 3,927.1 | — | 967,867.2 | — |
| `monocle_powerEach` | `size=64` | 2,069.6 ± 18.1 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,582.5 ± 227.2 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.9 ± 1.9 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,610.2 ± 3.9 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.1 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,282.1 ± 63.6 | — | 114,782.5 | — |
| `naive_powerEach` | `size=64` | 396.3 ± 3.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 63,879.1 ± 509.3 | — | 210,925.0 | — |
| `Modify_nested` | `size=16` | 1,676.4 ± 14.0 | — | 5,256.1 | — |
| `Modify_nested` | `size=256` | 16,489.5 ± 688.5 | — | 54,170.4 | — |
| `Modify_nested` | `size=4` | 818.8 ± 7.8 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,079.4 ± 197.8 | — | 15,101.8 | — |
| `monocle_nested` | `size=1024` | 253,723.8 ± 1,617.4 | — | 1,118,886.9 | — |
| `monocle_nested` | `size=16` | 2,888.2 ± 31.0 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 94,393.0 ± 1,204.6 | — | 430,213.0 | — |
| `monocle_nested` | `size=4` | 1,272.2 ± 34.2 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 9,104.7 ± 92.4 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 21,687.7 ± 1,013.5 | — | 115,073.6 | — |
| `naive_nested` | `size=16` | 391.1 ± 5.6 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,812.5 ± 32.9 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 135.0 ± 3.2 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,418.3 ± 13.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,542.7 ± 8.7 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,100.7 ± 564.7 | — | 104,801.4 | — |
| `Modify_sparse` | `size=32` | 524.1 ± 10.4 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,195.0 ± 80.3 | — | 24,903.3 | — |
| `Modify_sparse` | `size=8` | 176.6 ± 0.7 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,717.6 ± 31.1 | — | 24,717.5 | — |
| `monocle_sparse` | `size=2048` | 100,931.8 ± 1,038.9 | — | 476,034.0 | — |
| `monocle_sparse` | `size=32` | 1,022.2 ± 6.8 | — | 6,349.4 | — |
| `monocle_sparse` | `size=512` | 32,638.1 ± 501.5 | — | 156,443.6 | — |
| `monocle_sparse` | `size=8` | 317.1 ± 7.8 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 327.4 ± 1.4 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,419.7 ± 52.6 | — | 24,612.2 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,293.5 ± 3.9 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.5 ± 0.3 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 22.8 ± 2.4 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.6 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 143,914.1 ± 2,217.9 | — | 786,297.0 | — |
| `Cata` | `-` | 84,379.8 ± 388.8 | — | 197,568.6 | — |
| `Hylo` | `-` | 84,238.0 ± 383.7 | — | 295,848.6 | — |
| `drosteAna` | `-` | 54,325.4 ± 3,697.4 | — | 327,632.4 | — |
| `drosteCata` | `-` | 45,089.2 ± 692.2 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,632.7 ± 256.3 | — | 328,640.5 | — |
| `handAna` | `-` | 19,573.5 ± 124.8 | — | 163,816.1 | — |
| `handCata` | `-` | 13,114.5 ± 18.9 | — | 0.1 | — |
| `handHylo` | `-` | 11,338.4 ± 173.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.6 ± 0.1 | 25.9 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.0 ± 0.2 | 59.2 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.0 ± 0.1 | 3.0 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,089.4 ± 92.5 | 35,566.9 ± 223.8 | 39,001.1 | 176,924.5 |
| `Modify` | `size=64` | 955.9 ± 1.8 | 1,770.3 ± 6.7 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 110.0 ± 0.3 | 290.0 ± 6.5 | 728.0 | 1,936.0 |

