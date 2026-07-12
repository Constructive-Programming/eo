# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `6c70289ebc0793855500e4d2d2393563f04a22dc` · date: `2026-07-12` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 1.9 ± 0.0 | 0.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 11.8 ± 0.1 | 7.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 23.1 ± 0.2 | 18.5 ± 0.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.8 ± 0.0 | 0.8 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 120.3 ± 0.5 | — | 704.0 | — |
| `ModifyCountry` | `-` | 261.4 ± 1.8 | — | 3,448.0 | — |
| `ModifyPartner` | `-` | 304.0 ± 3.4 | — | 3,509.3 | — |
| `ReadCountry` | `-` | 140.9 ± 1.8 | — | 784.0 | — |
| `ReadPartner` | `-` | 166.2 ± 2.1 | — | 744.0 | — |
| `SliceGraftPayload` | `-` | 248.8 ± 3.4 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,100.8 ± 9.9 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,100.4 ± 29.5 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 3,270.9 ± 52.6 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,306.3 ± 19.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,313.4 ± 18.9 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 582.6 ± 8.3 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 418.3 ± 2.8 | — | 1,592.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,053.3 ± 43.3 | — | 10,840.0 | — |
| `ClickToJson` | `-` | 2,674.4 ± 33.2 | — | 5,456.0 | — |
| `WideToAvro` | `-` | 614.1 ± 10.5 | — | 7,144.0 | — |
| `WideToJson` | `-` | 470.0 ± 13.2 | — | 2,016.0 | — |
| `naiveClickToAvro` | `-` | 1,102.3 ± 3.8 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,227.3 ± 44.6 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 754.9 ± 6.9 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,566.8 ± 59.3 | — | 4,376.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 16.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 17.7 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.7 ± 1.3 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.3 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 26.4 ± 0.4 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 30.0 ± 0.4 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 4.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.1 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.3 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 17.3 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 33.8 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 17.5 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.8 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 37.8 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 106.3 ± 1.7 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 49.9 ± 0.5 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,901.4 ± 5.4 | 3,669.3 ± 4.7 | 14,080.6 | 14,080.6 |
| `FoldMap` | `size=64` | 251.8 ± 0.5 | 242.4 ± 0.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 15.9 ± 0.0 | 16.9 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,375.6 ± 8.4 | 2,393.4 ± 12.3 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 288.5 ± 0.4 | 288.4 ± 0.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 37.3 ± 0.0 | 37.6 ± 0.6 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.8 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.5 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.8 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 14.4 ± 0.3 | 6.8 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 25.0 ± 0.0 | 21.3 ± 0.1 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.9 ± 0.0 | 3.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 329,304.7 ± 1,671.6 | — | 1,072,991.4 | — |
| `cModifyId` | `size=64` | 43,272.6 ± 888.4 | — | 136,404.4 | — |
| `cModifyId` | `size=8` | 7,241.2 ± 201.0 | — | 21,072.1 | — |
| `cReadId` | `size=512` | 169,234.7 ± 361.6 | — | 804,168.1 | — |
| `cReadId` | `size=64` | 21,591.1 ± 113.8 | — | 101,368.6 | — |
| `cReadId` | `size=8` | 3,405.2 ± 7.8 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 169,036.0 ± 403.2 | — | 804,328.1 | — |
| `cReadStreet` | `size=64` | 21,446.9 ± 64.4 | — | 101,544.6 | — |
| `cReadStreet` | `size=8` | 3,441.5 ± 31.3 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 327,207.1 ± 791.4 | — | 1,072,928.1 | — |
| `cReplaceId` | `size=64` | 42,225.9 ± 292.9 | — | 136,327.8 | — |
| `cReplaceId` | `size=8` | 7,124.6 ± 163.8 | — | 20,760.1 | — |
| `cSumPrices` | `size=512` | 274,715.4 ± 1,559.4 | — | 1,255,800.7 | — |
| `cSumPrices` | `size=64` | 34,177.0 ± 110.2 | — | 158,180.7 | — |
| `cSumPrices` | `size=8` | 5,008.5 ± 15.4 | — | 22,928.0 | — |
| `jMiss` | `size=512` | 138.7 ± 0.5 | — | 0.0 | — |
| `jMiss` | `size=64` | 139.0 ± 1.0 | — | 0.0 | — |
| `jMiss` | `size=8` | 138.6 ± 0.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,485.3 ± 26.2 | — | 41,920.7 | — |
| `jModifyId` | `size=64` | 288.6 ± 1.5 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 82.4 ± 1.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 30.3 ± 1.9 | — | 48.0 | — |
| `jReadId` | `size=64` | 28.9 ± 1.2 | — | 56.0 | — |
| `jReadId` | `size=8` | 28.5 ± 1.4 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 148.0 ± 0.3 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 148.0 ± 0.4 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 147.8 ± 0.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,467.8 ± 8.9 | — | 41,888.7 | — |
| `jReplaceId` | `size=64` | 281.9 ± 2.9 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 78.4 ± 0.3 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 68,024.2 ± 425.2 | — | 63,673.7 | — |
| `jSumPrices` | `size=64` | 8,246.3 ± 62.3 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,114.7 ± 13.6 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.1 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 30.1 ± 0.1 | 25.5 ± 0.0 | 152.0 | 176.0 |
| `Replace` | `-` | 2.8 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 41,865.8 ± 424.6 | — | 380,271.7 | — |
| `Modify_multiFocus` | `size=256` | 10,303.6 ± 20.4 | — | 88,400.7 | — |
| `Modify_multiFocus` | `size=32` | 1,217.8 ± 2.6 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 176.6 ± 1.2 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 29,529.4 ± 90.6 | — | 119,391.4 | — |
| `Modify_powerEach` | `size=256` | 6,656.6 ± 804.8 | — | 27,184.5 | — |
| `Modify_powerEach` | `size=32` | 876.4 ± 3.4 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 170.6 ± 0.4 | — | 904.0 | — |
| `naive_listMap` | `size=1024` | 7,004.5 ± 25.4 | — | 65,577.8 | — |
| `naive_listMap` | `size=256` | 1,758.4 ± 8.2 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 214.2 ± 1.3 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 31.3 ± 0.3 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 53.5 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 142.7 ± 5.1 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 12.7 ± 0.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 20.6 ± 0.0 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 32.4 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 6.3 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 11.5 ± 0.0 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 133.1 ± 0.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 38.3 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 841.2 ± 10.5 | — | 3,152.0 | — |
| `reuseUse` | `-` | 798.7 ± 2.2 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 18.5 ± 0.1 | 17.5 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 49.7 ± 0.1 | 51.7 ± 0.1 | 160.0 | 304.0 |
| `Modify_6` | `-` | 117.2 ± 0.2 | 84.2 ± 0.2 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.4 ± 0.0 | 16.7 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.3 ± 0.0 | 2.9 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.1 ± 0.0 | 5.9 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 30,569.0 ± 120.5 | — | 97,655.2 | — |
| `ModifyNames` | `size=64` | 3,852.4 ± 10.0 | — | 12,792.1 | — |
| `ModifyNames` | `size=8` | 524.3 ± 0.7 | — | 2,208.0 | — |
| `ModifyStreet` | `size=512` | 112.1 ± 2.1 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 111.9 ± 0.6 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.8 ± 0.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.2 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.0 ± 0.5 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 81,279.4 ± 297.4 | — | 382,783.7 | — |
| `monocleModifyNames` | `size=64` | 8,089.2 ± 406.6 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,131.3 ± 7.1 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,661.2 ± 180.3 | — | 169,080.2 | — |
| `monocleModifyStreet` | `size=64` | 5,872.2 ± 17.1 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 754.8 ± 2.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 28,362.8 ± 206.2 | — | 69,784.7 | — |
| `monocleReadStreet` | `size=64` | 3,638.8 ± 17.2 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 422.4 ± 1.6 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 57,070.0 ± 215.8 | — | 226,294.8 | — |
| `naiveModifyNames` | `size=64` | 7,037.8 ± 443.9 | — | 27,928.2 | — |
| `naiveModifyNames` | `size=8` | 894.7 ± 4.1 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,622.8 ± 147.4 | — | 169,056.6 | — |
| `naiveModifyStreet` | `size=64` | 5,926.2 ± 22.5 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 759.9 ± 3.3 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 28,559.5 ± 88.6 | — | 69,784.7 | — |
| `naiveReadStreet` | `size=64` | 3,670.9 ± 6.4 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 422.0 ± 0.6 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 187,829.2 ± 1,607.8 | — | 655,014.5 | — |
| `Names` | `size=64` | 23,246.4 ± 199.1 | — | 82,938.4 | — |
| `Names` | `size=8` | 3,135.0 ± 16.7 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 201,806.8 ± 4,144.3 | — | 724,537.7 | — |
| `NamesIor` | `size=64` | 25,102.0 ± 772.2 | — | 91,178.4 | — |
| `NamesIor` | `size=8` | 3,271.7 ± 40.8 | — | 12,093.4 | — |
| `Street` | `size=512` | 806.4 ± 16.9 | — | 2,968.6 | — |
| `Street` | `size=64` | 807.3 ± 26.6 | — | 2,968.1 | — |
| `Street` | `size=8` | 792.8 ± 6.0 | — | 2,952.0 | — |
| `StreetIor` | `size=512` | 801.7 ± 6.4 | — | 2,984.6 | — |
| `StreetIor` | `size=64` | 804.4 ± 14.3 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 797.1 ± 6.8 | — | 2,984.0 | — |
| `directNames` | `size=512` | 186,464.8 ± 4,012.9 | — | 613,949.5 | — |
| `directNames` | `size=64` | 22,502.8 ± 86.1 | — | 77,719.7 | — |
| `directNames` | `size=8` | 3,045.8 ± 15.4 | — | 10,693.4 | — |
| `directStreet` | `size=512` | 776.4 ± 10.8 | — | 2,744.6 | — |
| `directStreet` | `size=64` | 772.4 ± 5.3 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 775.6 ± 11.2 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 188,733.1 ± 4,077.1 | — | 613,951.2 | — |
| `hcursorNames` | `size=64` | 22,737.5 ± 225.6 | — | 77,778.4 | — |
| `hcursorNames` | `size=8` | 3,088.1 ± 31.8 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 808.8 ± 5.9 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 813.7 ± 5.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 807.2 ± 5.6 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 193,055.8 ± 12,265.9 | — | 1,121,748.4 | — |
| `monocleNames` | `size=64` | 19,973.9 ± 38.8 | — | 132,772.5 | — |
| `monocleNames` | `size=8` | 2,958.8 ± 51.1 | — | 19,477.4 | — |
| `monocleStreet` | `size=512` | 147,502.4 ± 6,319.0 | — | 908,003.1 | — |
| `monocleStreet` | `size=64` | 17,337.8 ± 25.1 | — | 113,793.6 | — |
| `monocleStreet` | `size=8` | 2,511.2 ± 15.1 | — | 17,085.4 | — |
| `naiveNames` | `size=512` | 163,256.0 ± 7,593.0 | — | 965,245.7 | — |
| `naiveNames` | `size=64` | 18,906.5 ± 59.0 | — | 120,841.7 | — |
| `naiveNames` | `size=8` | 2,667.7 ± 23.3 | — | 17,856.1 | — |
| `naiveStreet` | `size=512` | 150,134.2 ± 5,305.0 | — | 908,004.9 | — |
| `naiveStreet` | `size=64` | 17,394.9 ± 62.6 | — | 113,780.2 | — |
| `naiveStreet` | `size=8` | 2,497.5 ± 11.9 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 2,672.9 ± 7.6 | — | 42,026.4 | — |
| `ModifyStreet` | `size=64` | 409.1 ± 5.4 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 215.6 ± 0.4 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 149.1 ± 1.8 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 148.9 ± 1.3 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 151.6 ± 5.8 | — | 128.0 | — |
| `SumPrices` | `size=512` | 68,088.0 ± 520.3 | — | 63,717.1 | — |
| `SumPrices` | `size=64` | 8,409.7 ± 17.2 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,116.9 ± 13.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 134,708.8 ± 1,206.0 | — | 333,516.3 | — |
| `monocleModifyStreet` | `size=64` | 16,888.3 ± 281.0 | — | 30,113.7 | — |
| `monocleModifyStreet` | `size=8` | 2,713.1 ± 21.9 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 76,381.8 ± 185.2 | — | 193,249.4 | — |
| `monocleReadStreet` | `size=64` | 9,685.8 ± 76.4 | — | 24,737.0 | — |
| `monocleReadStreet` | `size=8` | 1,508.4 ± 51.9 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 371,559.2 ± 1,739.5 | — | 1,190,734.0 | — |
| `monocleSumPrices` | `size=64` | 13,171.9 ± 28.0 | — | 47,417.3 | — |
| `monocleSumPrices` | `size=8` | 1,983.7 ± 36.0 | — | 6,664.0 | — |
| `naiveModifyStreet` | `size=512` | 134,478.6 ± 180.4 | — | 333,575.8 | — |
| `naiveModifyStreet` | `size=64` | 16,766.2 ± 62.1 | — | 30,089.7 | — |
| `naiveModifyStreet` | `size=8` | 2,701.1 ± 6.4 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 76,537.7 ± 554.2 | — | 193,249.5 | — |
| `naiveReadStreet` | `size=64` | 9,636.6 ± 20.7 | — | 24,737.0 | — |
| `naiveReadStreet` | `size=8` | 1,467.7 ± 3.5 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 80,394.8 ± 395.1 | — | 230,140.8 | — |
| `naiveSumPrices` | `size=64` | 10,169.0 ± 32.1 | — | 29,369.0 | — |
| `naiveSumPrices` | `size=8` | 1,551.6 ± 22.2 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 33,181.4 ± 447.4 | — | 451.3 | — |
| `nativeReadStreet` | `size=64` | 4,028.9 ± 147.2 | — | 424.4 | — |
| `nativeReadStreet` | `size=8` | 663.0 ± 10.1 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 53,024.1 ± 187.6 | — | 86,253.4 | — |
| `nativeSumPrices` | `size=64` | 6,559.0 ± 47.8 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 964.0 ± 2.9 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 106,254.0 ± 228.9 | — | 624,373.3 | — |
| `TransformDeep` | `n=512` | 10,572.5 ± 20.9 | — | 57,361.1 | — |
| `TransformDeep` | `n=64` | 1,235.8 ± 3.4 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 117,702.8 ± 1,593.5 | 143,419.8 ± 426.2 | 655,349.7 | 753,720.3 |
| `TransformExpr` | `n=512` | 14,512.0 ± 27.6 | 13,234.7 ± 45.9 | 81,825.5 | 69,585.4 |
| `TransformExpr` | `n=64` | 1,861.9 ± 109.5 | 2,201.4 ± 4.3 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 101,107.7 ± 657.0 | — | 786,577.6 | — |
| `UniverseDeep` | `n=512` | 12,493.2 ± 30.8 | — | 98,377.3 | — |
| `UniverseDeep` | `n=64` | 1,494.1 ± 1.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 95,278.1 ± 1,462.6 | 1,408,045.7 ± 11,723.4 | 786,381.3 | 4,752,240.0 |
| `UniverseExpr` | `n=512` | 12,055.1 ± 40.2 | 140,072.2 ± 2,975.2 | 98,185.2 | 483,198.3 |
| `UniverseExpr` | `n=64` | 1,479.2 ± 36.4 | 12,815.9 ± 147.1 | 12,168.0 | 46,448.2 |
| `UniverseJson` | `n=4096` | 150,573.7 ± 859.4 | 1,576,783.0 ± 58,568.8 | 786,421.6 | 6,488,690.8 |
| `UniverseJson` | `n=512` | 16,785.0 ± 36.1 | 160,818.4 ± 432.7 | 98,185.7 | 699,912.4 |
| `UniverseJson` | `n=64` | 1,977.3 ± 4.2 | 15,896.6 ± 63.6 | 12,168.0 | 73,208.3 |
| `visitorTransformDeep` | `n=4096` | 34,898.1 ± 497.6 | — | 163,881.4 | — |
| `visitorTransformDeep` | `n=512` | 3,305.6 ± 132.9 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 360.8 ± 5.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 56,441.7 ± 145.9 | — | 360,465.1 | — |
| `visitorTransformExpr` | `n=512` | 7,068.8 ± 42.7 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 867.8 ± 1.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 46,091.1 ± 922.3 | — | 196,697.5 | — |
| `visitorUniverseDeep` | `n=512` | 5,833.3 ± 183.4 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 645.5 ± 1.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 42,924.9 ± 63.4 | — | 196,647.2 | — |
| `visitorUniverseExpr` | `n=512` | 5,379.6 ± 19.1 | — | 24,584.5 | — |
| `visitorUniverseExpr` | `n=64` | 637.9 ± 1.3 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 99,566.8 ± 3,505.7 | — | 294,971.1 | — |
| `visitorUniverseJson` | `n=512` | 12,426.4 ± 2,136.9 | — | 36,849.3 | — |
| `visitorUniverseJson` | `n=64` | 1,989.5 ± 51.7 | — | 5,552.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 11,411.7 ± 23.3 | — | 41,437.4 | — |
| `Modify_powerEach` | `size=16` | 229.3 ± 2.7 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 2,751.8 ± 9.7 | — | 10,712.4 | — |
| `Modify_powerEach` | `size=4` | 102.4 ± 0.7 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 45,435.6 ± 195.3 | — | 164,411.7 | — |
| `Modify_powerEach` | `size=64` | 718.7 ± 1.4 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 45,305.3 ± 608.6 | — | 279,423.5 | — |
| `monocle_powerEach` | `size=16` | 436.1 ± 15.5 | — | 3,725.3 | — |
| `monocle_powerEach` | `size=256` | 16,910.0 ± 177.8 | — | 107,346.7 | — |
| `monocle_powerEach` | `size=4` | 180.4 ± 1.5 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 140,895.9 ± 1,415.9 | — | 967,795.3 | — |
| `monocle_powerEach` | `size=64` | 1,661.0 ± 3.4 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 4,546.0 ± 8.8 | — | 28,730.2 | — |
| `naive_powerEach` | `size=16` | 86.5 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,315.0 ± 15.1 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 22.4 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 17,403.5 ± 1,523.1 | — | 114,774.1 | — |
| `naive_powerEach` | `size=64` | 329.3 ± 0.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 51,386.5 ± 299.6 | — | 210,897.4 | — |
| `Modify_nested` | `size=16` | 1,262.5 ± 18.5 | — | 5,269.4 | — |
| `Modify_nested` | `size=256` | 13,385.0 ± 187.2 | — | 54,107.1 | — |
| `Modify_nested` | `size=4` | 592.5 ± 4.3 | — | 2,632.0 | — |
| `Modify_nested` | `size=64` | 3,384.7 ± 147.3 | — | 15,085.7 | — |
| `monocle_nested` | `size=1024` | 195,256.3 ± 3,473.5 | — | 1,118,774.7 | — |
| `monocle_nested` | `size=16` | 2,028.3 ± 56.5 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 70,701.9 ± 276.6 | — | 430,205.0 | — |
| `monocle_nested` | `size=4` | 994.4 ± 64.6 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 6,889.3 ± 165.5 | — | 58,912.8 | — |
| `naive_nested` | `size=1024` | 17,631.3 ± 113.1 | — | 115,065.8 | — |
| `naive_nested` | `size=16` | 318.0 ± 5.9 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,994.9 ± 27.4 | — | 29,018.6 | — |
| `naive_nested` | `size=4` | 111.6 ± 2.6 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,142.6 ± 17.8 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,351.8 ± 119.6 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 23,617.1 ± 284.0 | — | 104,795.0 | — |
| `Modify_sparse` | `size=32` | 419.8 ± 0.7 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 6,002.9 ± 56.6 | — | 24,903.0 | — |
| `Modify_sparse` | `size=8` | 135.4 ± 2.4 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,012.6 ± 4.1 | — | 24,717.5 | — |
| `monocle_sparse` | `size=2048` | 74,644.5 ± 444.9 | — | 476,022.4 | — |
| `monocle_sparse` | `size=32` | 753.9 ± 10.5 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 24,485.8 ± 249.9 | — | 156,437.8 | — |
| `monocle_sparse` | `size=8` | 211.0 ± 0.5 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 252.9 ± 6.4 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,345.1 ± 26.9 | — | 24,611.5 | — |
| `naive_sparse` | `size=32` | 64.7 ± 0.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,020.3 ± 1.7 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 20.1 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.9 ± 0.0 | 2.0 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.8 ± 0.0 | 1.9 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.8 ± 0.0 | 1.9 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 16.5 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 29.1 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 5.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 8.9 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 95,237.5 ± 1,262.4 | — | 786,296.7 | — |
| `Cata` | `-` | 67,670.7 ± 460.8 | — | 197,568.5 | — |
| `Hylo` | `-` | 65,444.4 ± 899.2 | — | 295,848.5 | — |
| `drosteAna` | `-` | 38,027.5 ± 597.4 | — | 327,632.3 | — |
| `drosteCata` | `-` | 36,439.0 ± 336.6 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 49,133.8 ± 597.7 | — | 328,640.3 | — |
| `handAna` | `-` | 16,688.9 ± 654.2 | — | 163,816.1 | — |
| `handCata` | `-` | 10,123.5 ± 95.6 | — | 0.1 | — |
| `handHylo` | `-` | 9,391.3 ± 8.2 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.9 ± 0.0 | 1.9 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 9.7 ± 0.2 | 20.4 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 20.4 ± 0.1 | 41.5 ± 0.9 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.7 ± 0.0 | 2.7 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 6,776.8 ± 109.2 | 28,562.6 ± 973.1 | 39,000.9 | 176,923.6 |
| `Modify` | `size=64` | 735.5 ± 2.6 | 1,411.1 ± 1.9 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 89.3 ± 0.1 | 188.1 ± 0.6 | 728.0 | 1,936.0 |

