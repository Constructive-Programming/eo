# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `c4423eb9449ab9dc65cdc9a767c94b5d1da94ba1` · date: `2026-07-17` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.6 ± 0.1 | 0.8 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.6 ± 0.1 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.0 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 10.2 ± 0.2 | 7.6 ± 0.2 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 22.5 ± 0.6 | 19.8 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.4 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 123.3 ± 2.4 | — | 704.0 | — |
| `ModifyCountry` | `-` | 573.3 ± 10.3 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 623.9 ± 8.5 | — | 3,245.3 | — |
| `ReadCountry` | `-` | 135.4 ± 3.8 | — | 520.0 | — |
| `ReadPartner` | `-` | 159.2 ± 3.4 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 240.5 ± 7.8 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,488.9 ± 43.2 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,572.0 ± 66.3 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 3,922.5 ± 89.6 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,448.1 ± 36.8 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,540.3 ± 28.2 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 766.9 ± 19.5 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 547.5 ± 10.0 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 306.3 ± 2.9 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 344.3 ± 4.8 | — | 1,504.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,233.1 ± 24.9 | — | 3,658.7 | — |
| `freshDecodeRecord` | `-` | 1,179.7 ± 10.9 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,591.6 ± 46.0 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,604.1 ± 27.9 | — | 4,032.0 | — |
| `WideToAvro` | `-` | 1,347.6 ± 17.3 | — | 6,536.0 | — |
| `WideToJson` | `-` | 550.0 ± 36.3 | — | 1,440.0 | — |
| `naiveClickToAvro` | `-` | 1,591.8 ± 76.7 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,521.4 ± 68.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,151.2 ± 17.1 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,726.4 ± 24.6 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 187.2 ± 2.4 | — | 984.0 | — |
| `decode_native` | `-` | 15.4 ± 0.4 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 185.5 ± 3.7 | — | 984.0 | — |
| `encode_bridged` | `-` | 205.4 ± 2.5 | — | 1,272.0 | — |
| `encode_native` | `-` | 12.3 ± 0.2 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 213.0 ± 5.5 | — | 1,277.3 | — |
| `fieldGet_bridged` | `-` | 86.7 ± 1.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 86.0 ± 1.0 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 345.4 ± 7.1 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 142.0 ± 1.1 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.4 ± 0.4 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 23.4 ± 0.3 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 20.5 ± 0.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.5 ± 0.4 | — | 0.0 | — |
| `getCap` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.1 | — | 0.0 | — |
| `getDirect` | `-` | 0.9 ± 0.1 | — | 0.0 | — |
| `modifyCap` | `-` | 5.6 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.3 ± 0.5 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 29.4 ± 0.8 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.9 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.7 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.7 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 10.5 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 28.8 ± 0.6 | — | 184.0 | — |
| `buildLens6` | `-` | 54.7 ± 0.5 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 28.4 ± 0.6 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.0 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 33.8 ± 0.5 | — | 72.0 | — |
| `reuseLens6` | `-` | 99.5 ± 1.7 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 47.9 ± 1.1 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,339.0 ± 65.5 | 3,255.0 ± 171.9 | 14,080.5 | 14,080.5 |
| `FoldMap` | `size=64` | 235.1 ± 2.8 | 228.8 ± 2.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.4 | 20.3 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,739.2 ± 28.6 | 2,982.7 ± 66.5 | 12,312.4 | 12,312.5 |
| `FoldPrices` | `size=64` | 306.6 ± 4.1 | 311.2 ± 3.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 41.8 ± 0.7 | 42.3 ± 1.6 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.0 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.4 ± 0.1 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 12.4 ± 0.2 | 6.4 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 24.5 ± 0.4 | 19.8 ± 0.9 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.8 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.8 ± 0.1 | 4.8 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.6 ± 0.1 | 4.6 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 324,046.8 ± 3,343.5 | — | 1,072,990.2 | — |
| `cModifyId` | `size=64` | 41,906.6 ± 454.7 | — | 136,404.4 | — |
| `cModifyId` | `size=8` | 7,138.7 ± 115.7 | — | 21,064.1 | — |
| `cReadId` | `size=512` | 175,356.7 ± 2,085.2 | — | 804,169.9 | — |
| `cReadId` | `size=64` | 21,993.7 ± 324.9 | — | 101,392.6 | — |
| `cReadId` | `size=8` | 3,571.3 ± 57.3 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 172,593.8 ± 3,275.8 | — | 804,337.1 | — |
| `cReadStreet` | `size=64` | 22,290.1 ± 374.0 | — | 101,544.6 | — |
| `cReadStreet` | `size=8` | 3,583.8 ± 46.4 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 314,468.5 ± 10,252.7 | — | 1,081,073.4 | — |
| `cReplaceId` | `size=64` | 40,258.7 ± 677.7 | — | 136,322.2 | — |
| `cReplaceId` | `size=8` | 6,945.9 ± 111.7 | — | 21,008.1 | — |
| `cSumPrices` | `size=512` | 270,929.4 ± 3,406.8 | — | 1,253,053.0 | — |
| `cSumPrices` | `size=64` | 34,442.5 ± 858.4 | — | 158,149.9 | — |
| `cSumPrices` | `size=8` | 5,389.5 ± 135.6 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 144.2 ± 11.6 | — | 0.0 | — |
| `jMiss` | `size=64` | 137.8 ± 1.3 | — | 0.0 | — |
| `jMiss` | `size=8` | 142.4 ± 3.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,834.1 ± 68.5 | — | 41,929.7 | — |
| `jModifyId` | `size=64` | 724.8 ± 10.6 | — | 5,344.0 | — |
| `jModifyId` | `size=8` | 154.7 ± 4.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 34.5 ± 0.5 | — | 48.0 | — |
| `jReadId` | `size=64` | 34.0 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 32.9 ± 0.3 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 157.6 ± 2.0 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 160.2 ± 2.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 164.0 ± 2.0 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 5,838.8 ± 77.7 | — | 41,897.7 | — |
| `jReplaceId` | `size=64` | 730.7 ± 31.4 | — | 5,312.0 | — |
| `jReplaceId` | `size=8` | 152.1 ± 1.4 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 75,938.8 ± 1,536.9 | — | 63,672.9 | — |
| `jSumPrices` | `size=64` | 9,505.4 ± 141.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,282.0 ± 30.1 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.6 ± 0.1 | 5.6 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 28.7 ± 0.5 | 29.4 ± 0.4 | 152.0 | 176.0 |
| `Replace` | `-` | 5.6 ± 0.1 | 5.5 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 69,575.4 ± 1,025.2 | — | 380,264.0 | — |
| `Modify_multiFocus` | `size=256` | 16,134.5 ± 290.0 | — | 88,422.4 | — |
| `Modify_multiFocus` | `size=32` | 1,843.4 ± 32.4 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 238.6 ± 7.5 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 32,687.8 ± 556.6 | — | 119,392.3 | — |
| `Modify_powerEach` | `size=256` | 7,678.1 ± 169.7 | — | 27,168.5 | — |
| `Modify_powerEach` | `size=32` | 844.7 ± 56.4 | — | 3,288.0 | — |
| `Modify_powerEach` | `size=4` | 169.4 ± 6.4 | — | 960.0 | — |
| `naive_listMap` | `size=1024` | 11,019.5 ± 146.4 | — | 65,578.9 | — |
| `naive_listMap` | `size=256` | 2,692.5 ± 48.0 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 344.0 ± 11.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 48.2 ± 1.1 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 47.9 ± 0.9 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 187.3 ± 2.9 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 19.9 ± 2.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 34.5 ± 0.6 | — | 224.0 | — |
| `naive_constSum` | `-` | 2.5 ± 0.1 | — | 16.0 | — |
| `naive_listSum` | `-` | 38.8 ± 0.7 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 13.2 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 24.6 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 210.5 ± 2.0 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 38.2 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 934.1 ± 64.3 | — | 3,152.0 | — |
| `reuseUse` | `-` | 896.5 ± 19.9 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.9 ± 0.3 | 20.6 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 47.3 ± 0.7 | 57.0 ± 0.9 | 160.0 | 304.0 |
| `Modify_6` | `-` | 114.3 ± 2.0 | 90.5 ± 1.5 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.1 ± 0.4 | 20.1 ± 0.3 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 5.8 ± 0.1 | 5.6 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 12.2 ± 0.2 | 12.0 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,334.2 ± 548.1 | — | 97,655.7 | — |
| `ModifyNames` | `size=64` | 4,107.0 ± 64.8 | — | 12,800.1 | — |
| `ModifyNames` | `size=8` | 592.4 ± 8.3 | — | 2,200.0 | — |
| `ModifyStreet` | `size=512` | 120.9 ± 2.0 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 121.0 ± 1.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 121.5 ± 1.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 36.5 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 36.4 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 36.5 ± 0.6 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 90,833.7 ± 847.9 | — | 382,778.4 | — |
| `monocleModifyNames` | `size=64` | 8,925.7 ± 399.8 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,340.5 ± 59.3 | — | 5,432.0 | — |
| `monocleModifyStreet` | `size=512` | 47,927.3 ± 558.6 | — | 169,081.4 | — |
| `monocleModifyStreet` | `size=64` | 6,247.5 ± 53.3 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 863.7 ± 8.1 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 28,100.9 ± 828.6 | — | 69,785.3 | — |
| `monocleReadStreet` | `size=64` | 3,619.3 ± 69.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 431.3 ± 7.0 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,887.7 ± 713.8 | — | 226,296.3 | — |
| `naiveModifyNames` | `size=64` | 7,478.7 ± 462.0 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,009.2 ± 26.5 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 47,809.8 ± 527.4 | — | 169,057.8 | — |
| `naiveModifyStreet` | `size=64` | 6,019.3 ± 406.4 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 869.6 ± 16.3 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,341.1 ± 193.5 | — | 69,782.3 | — |
| `naiveReadStreet` | `size=64` | 3,439.0 ± 42.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 424.0 ± 7.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 195,227.8 ± 2,200.2 | — | 650,913.7 | — |
| `Names` | `size=64` | 24,541.0 ± 776.6 | — | 82,946.5 | — |
| `Names` | `size=8` | 3,328.6 ± 71.8 | — | 11,360.1 | — |
| `NamesIor` | `size=512` | 218,981.0 ± 9,427.4 | — | 720,444.5 | — |
| `NamesIor` | `size=64` | 28,068.4 ± 525.2 | — | 91,176.5 | — |
| `NamesIor` | `size=8` | 3,637.2 ± 218.3 | — | 12,032.1 | — |
| `Street` | `size=512` | 918.3 ± 15.0 | — | 2,968.7 | — |
| `Street` | `size=64` | 927.5 ± 24.0 | — | 2,968.1 | — |
| `Street` | `size=8` | 908.3 ± 12.4 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 935.5 ± 14.6 | — | 2,984.7 | — |
| `StreetIor` | `size=64` | 941.3 ± 19.7 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 927.7 ± 26.6 | — | 2,984.0 | — |
| `directNames` | `size=512` | 193,040.6 ± 6,392.9 | — | 609,853.4 | — |
| `directNames` | `size=64` | 22,360.3 ± 1,183.4 | — | 77,714.4 | — |
| `directNames` | `size=8` | 2,963.7 ± 29.4 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 867.8 ± 14.1 | — | 2,736.7 | — |
| `directStreet` | `size=64` | 870.7 ± 17.8 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 879.5 ± 17.2 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 191,561.1 ± 2,379.0 | — | 613,978.4 | — |
| `hcursorNames` | `size=64` | 24,889.6 ± 581.7 | — | 77,779.6 | — |
| `hcursorNames` | `size=8` | 3,433.1 ± 148.7 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 941.2 ± 17.5 | — | 3,032.7 | — |
| `hcursorStreet` | `size=64` | 940.9 ± 31.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 972.1 ± 20.3 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 204,731.7 ± 4,462.8 | — | 1,121,750.9 | — |
| `monocleNames` | `size=64` | 24,026.9 ± 489.5 | — | 132,768.0 | — |
| `monocleNames` | `size=8` | 3,795.6 ± 60.2 | — | 19,504.1 | — |
| `monocleStreet` | `size=512` | 157,596.6 ± 2,026.3 | — | 908,025.9 | — |
| `monocleStreet` | `size=64` | 20,473.1 ± 1,277.5 | — | 113,804.5 | — |
| `monocleStreet` | `size=8` | 3,201.4 ± 65.1 | — | 17,069.4 | — |
| `naiveNames` | `size=512` | 170,780.3 ± 3,616.7 | — | 965,250.7 | — |
| `naiveNames` | `size=64` | 21,848.2 ± 302.5 | — | 120,836.6 | — |
| `naiveNames` | `size=8` | 3,375.8 ± 75.3 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 154,554.6 ± 2,424.3 | — | 908,013.2 | — |
| `naiveStreet` | `size=64` | 20,022.5 ± 455.0 | — | 113,791.1 | — |
| `naiveStreet` | `size=8` | 3,150.2 ± 40.2 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 6,005.0 ± 51.6 | — | 42,029.4 | — |
| `ModifyStreet` | `size=64` | 848.0 ± 13.5 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 250.2 ± 3.1 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 146.6 ± 1.5 | — | 101.5 | — |
| `ReadStreet` | `size=64` | 152.5 ± 2.6 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 154.7 ± 3.5 | — | 128.0 | — |
| `SumPrices` | `size=512` | 78,227.4 ± 2,570.2 | — | 63,721.4 | — |
| `SumPrices` | `size=64` | 8,865.8 ± 509.0 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,193.7 ± 23.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 141,185.8 ± 3,587.9 | — | 333,581.9 | — |
| `monocleModifyStreet` | `size=64` | 17,085.8 ± 380.6 | — | 30,113.7 | — |
| `monocleModifyStreet` | `size=8` | 2,966.1 ± 75.3 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 66,669.7 ± 270.1 | — | 193,241.1 | — |
| `monocleReadStreet` | `size=64` | 8,483.8 ± 29.2 | — | 24,736.9 | — |
| `monocleReadStreet` | `size=8` | 1,398.2 ± 35.1 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 324,570.1 ± 9,677.3 | — | 1,190,693.8 | — |
| `monocleSumPrices` | `size=64` | 13,171.3 ± 393.2 | — | 47,420.0 | — |
| `monocleSumPrices` | `size=8` | 2,196.2 ± 176.2 | — | 6,680.0 | — |
| `naiveModifyStreet` | `size=512` | 142,266.5 ± 2,143.4 | — | 333,512.9 | — |
| `naiveModifyStreet` | `size=64` | 17,239.2 ± 582.0 | — | 30,089.7 | — |
| `naiveModifyStreet` | `size=8` | 2,745.7 ± 151.2 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 73,670.4 ± 1,855.8 | — | 193,247.1 | — |
| `naiveReadStreet` | `size=64` | 9,465.2 ± 162.7 | — | 24,737.0 | — |
| `naiveReadStreet` | `size=8` | 1,513.5 ± 30.8 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 81,833.8 ± 1,431.7 | — | 230,142.0 | — |
| `naiveSumPrices` | `size=64` | 9,964.5 ± 130.8 | — | 29,369.0 | — |
| `naiveSumPrices` | `size=8` | 1,549.5 ± 27.9 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 34,818.2 ± 692.4 | — | 453.1 | — |
| `nativeReadStreet` | `size=64` | 4,480.6 ± 301.9 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 783.6 ± 22.1 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 53,999.2 ± 558.9 | — | 86,254.7 | — |
| `nativeSumPrices` | `size=64` | 6,816.2 ± 146.5 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 994.7 ± 9.2 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 136,905.8 ± 2,090.5 | — | 624,395.6 | — |
| `TransformDeep` | `n=512` | 14,252.2 ± 177.3 | — | 57,361.5 | — |
| `TransformDeep` | `n=64` | 1,575.8 ± 22.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 139,926.5 ± 1,009.2 | 159,954.0 ± 2,166.0 | 655,365.8 | 753,732.4 |
| `TransformExpr` | `n=512` | 17,714.8 ± 417.8 | 14,494.4 ± 294.8 | 81,825.8 | 69,585.5 |
| `TransformExpr` | `n=64` | 2,118.4 ± 33.9 | 2,429.0 ± 32.2 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 158,851.1 ± 27,266.0 | — | 786,619.4 | — |
| `UniverseDeep` | `n=512` | 19,431.3 ± 392.6 | — | 98,378.0 | — |
| `UniverseDeep` | `n=64` | 2,266.1 ± 33.2 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 157,301.4 ± 3,008.8 | 3,575,868.8 ± 36,999.1 | 786,426.5 | 4,753,814.8 |
| `UniverseExpr` | `n=512` | 19,151.6 ± 151.5 | 349,997.6 ± 4,132.4 | 98,186.0 | 483,219.7 |
| `UniverseExpr` | `n=64` | 2,203.5 ± 44.2 | 32,625.7 ± 485.3 | 12,168.0 | 46,448.6 |
| `UniverseJson` | `n=4096` | 190,002.9 ± 3,339.9 | 3,621,981.5 ± 121,084.0 | 786,450.3 | 6,490,173.7 |
| `UniverseJson` | `n=512` | 22,695.8 ± 289.8 | 362,491.8 ± 5,689.6 | 98,186.3 | 699,933.0 |
| `UniverseJson` | `n=64` | 2,546.9 ± 58.7 | 35,801.5 ± 255.1 | 12,168.0 | 73,208.7 |
| `visitorTransformDeep` | `n=4096` | 43,345.9 ± 848.0 | — | 163,887.5 | — |
| `visitorTransformDeep` | `n=512` | 4,696.8 ± 102.0 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 548.7 ± 11.8 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 71,775.2 ± 818.3 | — | 360,476.2 | — |
| `visitorTransformExpr` | `n=512` | 8,278.1 ± 212.3 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,085.0 ± 15.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 64,025.8 ± 1,642.6 | — | 196,710.6 | — |
| `visitorUniverseDeep` | `n=512` | 7,808.2 ± 155.4 | — | 24,632.8 | — |
| `visitorUniverseDeep` | `n=64` | 826.8 ± 13.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 56,080.4 ± 1,579.4 | — | 196,656.8 | — |
| `visitorUniverseExpr` | `n=512` | 7,113.0 ± 186.5 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 836.0 ± 12.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 97,368.9 ± 1,670.7 | — | 294,966.9 | — |
| `visitorUniverseJson` | `n=512` | 11,203.9 ± 58.5 | — | 36,849.2 | — |
| `visitorUniverseJson` | `n=64` | 2,133.0 ± 374.8 | — | 5,552.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,038.3 ± 167.8 | — | 41,438.2 | — |
| `Modify_powerEach` | `size=16` | 213.0 ± 1.2 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,118.2 ± 137.4 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 104.2 ± 0.8 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 53,293.4 ± 760.3 | — | 164,425.9 | — |
| `Modify_powerEach` | `size=64` | 789.2 ± 33.8 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 58,749.4 ± 487.1 | — | 279,433.4 | — |
| `monocle_powerEach` | `size=16` | 697.2 ± 18.2 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,272.2 ± 313.0 | — | 107,328.7 | — |
| `monocle_powerEach` | `size=4` | 204.0 ± 3.2 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 195,880.0 ± 3,921.7 | — | 967,890.2 | — |
| `monocle_powerEach` | `size=64` | 2,476.0 ± 34.1 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,445.3 ± 34.7 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 96.2 ± 1.6 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,366.0 ± 10.0 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 28.5 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,519.6 ± 1,426.3 | — | 114,782.9 | — |
| `naive_powerEach` | `size=64` | 350.4 ± 2.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 62,752.5 ± 3,468.2 | — | 210,971.8 | — |
| `Modify_nested` | `size=16` | 1,396.5 ± 23.0 | — | 5,256.1 | — |
| `Modify_nested` | `size=256` | 15,631.3 ± 160.5 | — | 54,121.8 | — |
| `Modify_nested` | `size=4` | 729.1 ± 18.2 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 3,920.8 ± 50.6 | — | 14,957.8 | — |
| `monocle_nested` | `size=1024` | 256,603.1 ± 43,373.6 | — | 1,118,892.4 | — |
| `monocle_nested` | `size=16` | 2,822.5 ± 35.7 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 84,832.9 ± 1,683.0 | — | 430,209.9 | — |
| `monocle_nested` | `size=4` | 1,122.4 ± 67.3 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 9,878.4 ± 139.6 | — | 58,913.1 | — |
| `naive_nested` | `size=1024` | 21,953.2 ± 296.1 | — | 115,074.1 | — |
| `naive_nested` | `size=16` | 400.0 ± 5.5 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,410.0 ± 89.5 | — | 29,019.6 | — |
| `naive_nested` | `size=4` | 139.6 ± 2.1 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,403.3 ± 25.2 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,405.7 ± 17.1 | — | 4,933.4 | — |
| `Modify_sparse` | `size=2048` | 27,532.7 ± 2,043.9 | — | 104,797.8 | — |
| `Modify_sparse` | `size=32` | 412.6 ± 10.7 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 6,965.1 ± 86.5 | — | 24,905.9 | — |
| `Modify_sparse` | `size=8` | 133.1 ± 1.3 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 4,283.2 ± 49.3 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 104,560.6 ± 2,644.9 | — | 476,035.2 | — |
| `monocle_sparse` | `size=32` | 1,111.4 ± 14.6 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 30,408.3 ± 237.3 | — | 156,441.8 | — |
| `monocle_sparse` | `size=8` | 310.9 ± 3.2 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 329.6 ± 3.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,676.2 ± 50.9 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 80.4 ± 1.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,420.1 ± 13.3 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.1 ± 0.4 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.7 ± 0.0 | 2.8 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.7 ± 0.1 | 2.6 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.7 ± 0.1 | 2.6 ± 0.1 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.6 ± 0.1 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 15.8 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 27.9 ± 0.6 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.2 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 16.9 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 138,340.5 ± 3,082.9 | — | 786,297.0 | — |
| `Cata` | `-` | 70,525.7 ± 1,536.0 | — | 197,568.5 | — |
| `Hylo` | `-` | 75,428.8 ± 1,778.2 | — | 295,848.5 | — |
| `drosteAna` | `-` | 57,255.9 ± 956.9 | — | 327,632.4 | — |
| `drosteCata` | `-` | 43,567.0 ± 778.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 60,700.5 ± 546.3 | — | 328,640.4 | — |
| `handAna` | `-` | 30,422.9 ± 290.7 | — | 163,816.2 | — |
| `handCata` | `-` | 14,072.2 ± 202.8 | — | 0.1 | — |
| `handHylo` | `-` | 11,517.9 ± 62.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.5 ± 0.3 | 3.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.0 ± 0.2 | 28.1 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 21.8 ± 0.2 | 50.8 ± 0.7 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 5.4 ± 0.1 | 5.4 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,467.2 ± 122.3 | 35,875.4 ± 2,261.0 | 39,001.1 | 176,912.8 |
| `Modify` | `size=64` | 932.3 ± 20.3 | 2,324.6 ± 24.2 | 4,904.0 | 14,448.1 |
| `Modify` | `size=8` | 118.0 ± 1.4 | 349.1 ± 8.1 | 728.0 | 1,936.0 |

