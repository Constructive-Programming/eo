# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `35ac9db547d9d415c22e03d747179899c7eb60af` · date: `2026-07-11` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.1 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.7 ± 0.3 | 23.6 ± 1.0 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 162.5 ± 5.1 | — | 704.0 | — |
| `ModifyCountry` | `-` | 383.7 ± 11.2 | — | 3,458.7 | — |
| `ModifyPartner` | `-` | 474.4 ± 15.5 | — | 3,504.0 | — |
| `ReadCountry` | `-` | 183.1 ± 4.4 | — | 784.0 | — |
| `ReadPartner` | `-` | 218.5 ± 4.4 | — | 744.0 | — |
| `SliceGraftPayload` | `-` | 323.1 ± 7.2 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,657.3 ± 7.3 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,620.2 ± 13.2 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,083.1 ± 147.2 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,678.8 ± 11.1 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,697.1 ± 14.0 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 763.9 ± 8.3 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 570.8 ± 4.7 | — | 1,592.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,755.3 ± 13.1 | — | 10,802.7 | — |
| `ClickToJson` | `-` | 3,373.4 ± 32.0 | — | 5,456.0 | — |
| `WideToAvro` | `-` | 976.2 ± 22.2 | — | 7,144.0 | — |
| `WideToJson` | `-` | 704.2 ± 26.8 | — | 1,994.7 | — |
| `naiveClickToAvro` | `-` | 1,751.4 ± 19.5 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,739.7 ± 23.5 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,009.5 ± 8.8 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,912.6 ± 32.7 | — | 4,376.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.6 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.4 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.9 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 41.4 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.0 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 134.0 ± 0.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.7 ± 0.7 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,681.8 ± 197.9 | 4,493.6 ± 47.6 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 324.4 ± 0.3 | 305.7 ± 1.0 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.1 | 20.4 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,767.3 ± 3.6 | 2,764.5 ± 7.7 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 348.9 ± 2.3 | 352.6 ± 1.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.1 ± 0.1 | 44.6 ± 0.5 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
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
| `Get_3` | `-` | 17.0 ± 0.0 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.1 ± 0.1 | 25.1 ± 1.1 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.9 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 405,681.9 ± 3,794.8 | — | 1,073,038.0 | — |
| `cModifyId` | `size=64` | 54,298.8 ± 1,163.2 | — | 136,406.1 | — |
| `cModifyId` | `size=8` | 8,852.6 ± 30.5 | — | 20,824.1 | — |
| `cReadId` | `size=512` | 219,805.6 ± 7,611.0 | — | 804,182.5 | — |
| `cReadId` | `size=64` | 27,156.9 ± 247.5 | — | 101,396.9 | — |
| `cReadId` | `size=8` | 4,454.6 ± 98.1 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 217,762.6 ± 3,314.3 | — | 804,349.9 | — |
| `cReadStreet` | `size=64` | 27,564.0 ± 219.8 | — | 101,557.0 | — |
| `cReadStreet` | `size=8` | 4,462.8 ± 55.0 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 408,863.2 ± 7,145.7 | — | 1,072,956.4 | — |
| `cReplaceId` | `size=64` | 52,295.7 ± 763.8 | — | 136,322.7 | — |
| `cReplaceId` | `size=8` | 8,854.0 ± 130.7 | — | 20,752.1 | — |
| `cSumPrices` | `size=512` | 351,320.5 ± 3,238.8 | — | 1,253,091.9 | — |
| `cSumPrices` | `size=64` | 43,885.4 ± 570.6 | — | 157,824.4 | — |
| `cSumPrices` | `size=8` | 6,515.0 ± 72.1 | — | 22,920.1 | — |
| `jMiss` | `size=512` | 165.8 ± 2.9 | — | 0.0 | — |
| `jMiss` | `size=64` | 164.1 ± 0.6 | — | 0.0 | — |
| `jMiss` | `size=8` | 164.6 ± 0.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,268.2 ± 41.9 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 416.2 ± 3.1 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 107.3 ± 2.4 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.1 ± 1.8 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.1 ± 1.9 | — | 56.0 | — |
| `jReadId` | `size=8` | 37.6 ± 3.8 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 180.4 ± 1.9 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 189.4 ± 12.4 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 187.7 ± 13.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,224.3 ± 28.6 | — | 41,888.9 | — |
| `jReplaceId` | `size=64` | 408.1 ± 4.1 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 100.3 ± 0.9 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 86,268.1 ± 651.9 | — | 63,664.9 | — |
| `jSumPrices` | `size=64` | 10,527.4 ± 415.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,455.0 ± 5.5 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.4 ± 0.1 | 31.5 ± 0.3 | 152.0 | 176.0 |
| `Replace` | `-` | 3.2 ± 0.0 | 3.1 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,795.5 ± 535.6 | — | 380,269.4 | — |
| `Modify_multiFocus` | `size=256` | 12,998.9 ± 123.5 | — | 88,422.2 | — |
| `Modify_multiFocus` | `size=32` | 1,539.8 ± 12.3 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 249.4 ± 5.0 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,848.5 ± 316.4 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,871.9 ± 45.0 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,096.7 ± 3.5 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 214.9 ± 0.5 | — | 904.0 | — |
| `naive_listMap` | `size=1024` | 8,306.7 ± 19.8 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,053.2 ± 7.4 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 247.3 ± 1.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.0 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.7 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 166.1 ± 1.6 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.8 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 38.5 ± 2.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.1 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.3 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 144.7 ± 0.7 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.9 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,259.3 ± 16.9 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,175.0 ± 19.3 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.2 ± 0.3 | 22.3 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.4 ± 0.3 | 70.4 ± 0.2 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.4 ± 1.2 | 116.9 ± 5.1 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.6 ± 0.1 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.9 ± 0.0 | 6.7 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,005.0 ± 283.7 | — | 97,656.7 | — |
| `ModifyNames` | `size=64` | 4,689.4 ± 58.0 | — | 12,792.2 | — |
| `ModifyNames` | `size=8` | 661.7 ± 3.9 | — | 2,200.0 | — |
| `ModifyStreet` | `size=512` | 133.2 ± 0.5 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 133.6 ± 1.9 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 135.9 ± 1.5 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 41.0 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.7 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.6 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,694.0 ± 919.3 | — | 382,772.2 | — |
| `monocleModifyNames` | `size=64` | 9,812.2 ± 76.1 | — | 39,832.3 | — |
| `monocleModifyNames` | `size=8` | 1,532.4 ± 27.8 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,371.5 ± 410.0 | — | 169,084.7 | — |
| `monocleModifyStreet` | `size=64` | 7,237.9 ± 22.0 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 1,029.6 ± 10.8 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,661.2 ± 162.5 | — | 69,789.2 | — |
| `monocleReadStreet` | `size=64` | 4,194.9 ± 24.0 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 509.4 ± 3.2 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,052.5 ± 348.4 | — | 226,301.2 | — |
| `naiveModifyNames` | `size=64` | 8,999.8 ± 129.0 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,214.7 ± 4.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,164.5 ± 294.0 | — | 169,061.7 | — |
| `naiveModifyStreet` | `size=64` | 7,228.6 ± 71.9 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 1,024.3 ± 10.7 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,750.4 ± 138.1 | — | 69,789.0 | — |
| `naiveReadStreet` | `size=64` | 4,217.8 ± 8.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 507.0 ± 1.8 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 272,420.1 ± 6,294.3 | — | 646,874.3 | — |
| `Names` | `size=64` | 34,791.0 ± 784.1 | — | 82,947.5 | — |
| `Names` | `size=8` | 4,613.0 ± 26.2 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 306,514.6 ± 4,227.0 | — | 724,621.6 | — |
| `NamesIor` | `size=64` | 37,796.1 ± 218.2 | — | 90,639.1 | — |
| `NamesIor` | `size=8` | 5,071.6 ± 25.6 | — | 12,032.1 | — |
| `Street` | `size=512` | 1,173.8 ± 4.9 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,168.1 ± 23.6 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,148.2 ± 22.6 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,177.4 ± 38.2 | — | 2,976.9 | — |
| `StreetIor` | `size=64` | 1,174.2 ± 15.7 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,169.1 ± 19.7 | — | 2,984.0 | — |
| `directNames` | `size=512` | 261,824.5 ± 3,870.5 | — | 609,905.8 | — |
| `directNames` | `size=64` | 32,787.0 ± 555.7 | — | 77,737.6 | — |
| `directNames` | `size=8` | 4,551.8 ± 69.2 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 1,108.1 ± 27.5 | — | 2,736.9 | — |
| `directStreet` | `size=64` | 1,091.3 ± 6.5 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,101.6 ± 22.5 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 266,753.6 ± 13,004.8 | — | 609,909.7 | — |
| `hcursorNames` | `size=64` | 32,816.9 ± 715.8 | — | 77,792.2 | — |
| `hcursorNames` | `size=8` | 4,556.6 ± 53.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,185.1 ± 16.7 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,193.3 ± 40.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,176.3 ± 20.9 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 239,739.0 ± 5,109.0 | — | 1,121,769.1 | — |
| `monocleNames` | `size=64` | 26,226.2 ± 574.3 | — | 132,760.0 | — |
| `monocleNames` | `size=8` | 4,002.3 ± 67.1 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 190,307.1 ± 3,364.1 | — | 908,031.9 | — |
| `monocleStreet` | `size=64` | 22,679.2 ± 170.9 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,402.4 ± 4.7 | — | 17,064.1 | — |
| `naiveNames` | `size=512` | 207,784.5 ± 5,522.8 | — | 965,286.2 | — |
| `naiveNames` | `size=64` | 24,414.9 ± 244.5 | — | 120,837.0 | — |
| `naiveNames` | `size=8` | 3,687.8 ± 96.5 | — | 17,818.7 | — |
| `naiveStreet` | `size=512` | 189,735.0 ± 2,022.6 | — | 908,031.5 | — |
| `naiveStreet` | `size=64` | 22,691.5 ± 109.9 | — | 113,796.7 | — |
| `naiveStreet` | `size=8` | 3,424.4 ± 23.5 | — | 17,056.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,657.9 ± 27.6 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 580.5 ± 5.6 | — | 5,440.1 | — |
| `ModifyStreet` | `size=8` | 288.4 ± 3.3 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 180.6 ± 1.7 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 179.1 ± 1.2 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 182.1 ± 3.8 | — | 128.0 | — |
| `SumPrices` | `size=512` | 85,080.3 ± 1,540.3 | — | 63,719.7 | — |
| `SumPrices` | `size=64` | 10,749.3 ± 37.1 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,457.1 ± 47.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 166,868.1 ± 2,442.0 | — | 333,612.1 | — |
| `monocleModifyStreet` | `size=64` | 20,584.4 ± 153.0 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,381.1 ± 12.8 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 94,949.4 ± 776.4 | — | 193,265.3 | — |
| `monocleReadStreet` | `size=64` | 12,192.0 ± 56.9 | — | 24,737.2 | — |
| `monocleReadStreet` | `size=8` | 1,922.6 ± 6.8 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 451,324.6 ± 4,073.0 | — | 1,190,803.3 | — |
| `monocleSumPrices` | `size=64` | 16,641.0 ± 44.1 | — | 47,417.7 | — |
| `monocleSumPrices` | `size=8` | 2,625.2 ± 30.1 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 166,125.5 ± 1,497.7 | — | 333,602.8 | — |
| `naiveModifyStreet` | `size=64` | 20,650.4 ± 250.4 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,370.1 ± 13.8 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 95,297.3 ± 393.8 | — | 193,265.6 | — |
| `naiveReadStreet` | `size=64` | 12,167.2 ± 58.1 | — | 24,737.2 | — |
| `naiveReadStreet` | `size=8` | 1,920.9 ± 5.7 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 100,921.6 ± 1,900.3 | — | 230,158.4 | — |
| `naiveSumPrices` | `size=64` | 12,842.8 ± 172.9 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,016.3 ± 11.6 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 38,425.0 ± 1,661.0 | — | 455.9 | — |
| `nativeReadStreet` | `size=64` | 4,770.3 ± 18.3 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 804.7 ± 5.9 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,120.0 ± 606.7 | — | 86,268.8 | — |
| `nativeSumPrices` | `size=64` | 8,002.9 ± 50.9 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,212.9 ± 22.8 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 130,709.1 ± 819.1 | — | 624,391.1 | — |
| `TransformDeep` | `n=512` | 13,057.6 ± 173.5 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,605.0 ± 4.7 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 147,328.4 ± 3,758.3 | 176,925.6 ± 1,006.1 | 655,371.2 | 753,744.8 |
| `TransformExpr` | `n=512` | 18,194.6 ± 130.4 | 15,990.0 ± 150.5 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,254.8 ± 5.6 | 2,709.2 ± 16.6 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 98,217.8 ± 1,057.5 | — | 786,575.5 | — |
| `UniverseDeep` | `n=512` | 15,325.7 ± 27.2 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,871.7 ± 16.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 97,043.1 ± 679.3 | 1,722,975.7 ± 71,826.4 | 786,382.6 | 4,752,468.9 |
| `UniverseExpr` | `n=512` | 14,434.0 ± 38.2 | 177,609.3 ± 5,567.4 | 98,185.5 | 483,202.1 |
| `UniverseExpr` | `n=64` | 1,754.1 ± 6.3 | 16,007.8 ± 211.6 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 180,974.9 ± 1,685.3 | 1,962,990.6 ± 110,395.2 | 786,443.7 | 6,488,971.4 |
| `UniverseJson` | `n=512` | 20,595.7 ± 518.5 | 209,939.4 ± 1,429.7 | 98,186.1 | 699,917.4 |
| `UniverseJson` | `n=64` | 2,463.6 ± 6.4 | 20,596.2 ± 470.5 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,992.7 ± 253.1 | — | 163,881.5 | — |
| `visitorTransformDeep` | `n=512` | 4,109.5 ± 37.7 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 444.1 ± 8.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,569.1 ± 685.9 | — | 360,473.2 | — |
| `visitorTransformExpr` | `n=512` | 8,305.3 ± 19.6 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,037.4 ± 8.2 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,371.5 ± 551.4 | — | 196,705.0 | — |
| `visitorUniverseDeep` | `n=512` | 6,857.5 ± 55.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 803.9 ± 3.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,204.2 ± 369.3 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,815.1 ± 33.9 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 836.8 ± 1.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 141,516.7 ± 868.7 | — | 294,999.0 | — |
| `visitorUniverseJson` | `n=512` | 13,839.7 ± 84.8 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,297.6 ± 259.6 | — | 5,216.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,820.1 ± 87.1 | — | 41,438.5 | — |
| `Modify_powerEach` | `size=16` | 295.3 ± 5.9 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,306.0 ± 18.4 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 145.5 ± 10.9 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 56,645.3 ± 580.8 | — | 164,429.6 | — |
| `Modify_powerEach` | `size=64` | 890.4 ± 7.6 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 59,767.3 ± 676.5 | — | 279,434.2 | — |
| `monocle_powerEach` | `size=16` | 643.0 ± 14.7 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,721.4 ± 55.7 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 243.1 ± 23.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 186,180.4 ± 1,739.0 | — | 967,873.5 | — |
| `monocle_powerEach` | `size=64` | 2,062.8 ± 7.5 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,506.8 ± 12.6 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 0.9 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,614.6 ± 11.7 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.0 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,208.7 ± 68.7 | — | 114,782.3 | — |
| `naive_powerEach` | `size=64` | 393.8 ± 0.7 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 63,699.8 ± 362.3 | — | 210,924.5 | — |
| `Modify_nested` | `size=16` | 1,671.6 ± 17.0 | — | 5,282.7 | — |
| `Modify_nested` | `size=256` | 15,725.4 ± 162.8 | — | 54,127.2 | — |
| `Modify_nested` | `size=4` | 820.9 ± 4.8 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,191.1 ± 240.3 | — | 15,005.8 | — |
| `monocle_nested` | `size=1024` | 252,291.3 ± 3,375.3 | — | 1,118,884.1 | — |
| `monocle_nested` | `size=16` | 2,882.1 ± 52.9 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 93,704.4 ± 1,692.1 | — | 430,212.8 | — |
| `monocle_nested` | `size=4` | 1,257.4 ± 41.3 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 8,908.0 ± 259.2 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 21,509.5 ± 980.0 | — | 115,073.3 | — |
| `naive_nested` | `size=16` | 387.7 ± 5.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,792.1 ± 30.7 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 135.4 ± 4.1 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,409.5 ± 13.3 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,583.3 ± 42.7 | — | 4,930.8 | — |
| `Modify_sparse` | `size=2048` | 28,536.7 ± 368.8 | — | 104,801.1 | — |
| `Modify_sparse` | `size=32` | 515.1 ± 1.8 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,093.8 ± 26.2 | — | 24,905.9 | — |
| `Modify_sparse` | `size=8` | 174.9 ± 1.6 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,696.1 ± 37.3 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 99,318.4 ± 647.9 | — | 476,032.8 | — |
| `monocle_sparse` | `size=32` | 1,020.0 ± 27.6 | — | 6,349.4 | — |
| `monocle_sparse` | `size=512` | 32,171.7 ± 391.9 | — | 156,443.2 | — |
| `monocle_sparse` | `size=8` | 317.2 ± 13.7 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 324.7 ± 0.9 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,417.7 ± 38.1 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 83.7 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,291.7 ± 10.7 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.1 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.2 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 36.8 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.3 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.5 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 142,711.0 ± 857.8 | — | 786,297.0 | — |
| `Cata` | `-` | 84,775.4 ± 735.6 | — | 197,568.6 | — |
| `Hylo` | `-` | 84,181.9 ± 831.3 | — | 295,848.6 | — |
| `drosteAna` | `-` | 54,729.1 ± 4,171.8 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,369.6 ± 142.9 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,516.3 ± 512.7 | — | 328,640.5 | — |
| `handAna` | `-` | 19,881.8 ± 998.0 | — | 163,816.1 | — |
| `handCata` | `-` | 13,122.1 ± 30.5 | — | 0.1 | — |
| `handHylo` | `-` | 11,329.9 ± 160.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.5 ± 0.0 | 25.8 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.1 ± 0.3 | 60.0 ± 1.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.9 ± 0.0 | 2.9 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,061.3 ± 66.8 | 34,687.8 ± 1,436.5 | 39,001.1 | 176,912.6 |
| `Modify` | `size=64` | 959.7 ± 5.7 | 1,757.2 ± 9.5 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 110.5 ± 2.1 | 287.1 ± 3.7 | 728.0 | 1,936.0 |

