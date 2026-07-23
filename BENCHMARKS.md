# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `0c0ae13595111ab686344b62e747e78c15bc5e02` · date: `2026-07-23` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.1 ± 0.3 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.7 ± 0.2 | 23.4 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 163.3 ± 1.6 | — | 720.0 | — |
| `ModifyCountry` | `-` | 379.6 ± 19.7 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 458.4 ± 9.9 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 171.5 ± 2.8 | — | 520.0 | — |
| `ReadPartner` | `-` | 212.8 ± 8.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 328.5 ± 8.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,708.1 ± 45.0 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,702.8 ± 147.4 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,166.9 ± 70.1 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,687.1 ± 16.2 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,705.9 ± 16.3 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 758.8 ± 7.4 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 583.4 ± 20.4 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 404.0 ± 3.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 425.4 ± 3.0 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,321.1 ± 4.2 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,285.1 ± 9.0 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,405.8 ± 41.7 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,961.2 ± 33.3 | — | 4,032.0 | — |
| `WideToAvro` | `-` | 1,002.9 ± 32.1 | — | 6,562.7 | — |
| `WideToJson` | `-` | 737.4 ± 33.2 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,756.7 ± 17.7 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,778.4 ± 35.2 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,015.9 ± 7.3 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,930.3 ± 33.9 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 226.1 ± 3.3 | — | 984.0 | — |
| `decode_native` | `-` | 18.5 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 221.9 ± 3.5 | — | 984.0 | — |
| `encode_bridged` | `-` | 256.0 ± 5.3 | — | 1,277.3 | — |
| `encode_native` | `-` | 14.7 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 262.8 ± 10.4 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 95.6 ± 1.0 | — | 432.0 | — |
| `fieldGet_native` | `-` | 104.7 ± 5.2 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 438.8 ± 14.8 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 177.3 ± 5.0 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.1 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.7 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.3 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.4 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.8 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.4 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 42.7 ± 0.6 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.6 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.2 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 136.0 ± 2.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.3 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,667.6 ± 187.3 | 4,466.5 ± 32.6 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 324.9 ± 0.9 | 307.2 ± 1.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.0 | 20.3 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,810.7 ± 95.8 | 2,774.6 ± 18.6 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 351.4 ± 3.7 | 353.1 ± 1.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.4 ± 0.3 | 44.6 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.0 ± 0.1 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 35.5 ± 2.2 | 25.7 ± 0.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.1 | 3.8 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.2 ± 0.1 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 409,778.5 ± 4,873.8 | — | 1,072,922.5 | — |
| `cModifyId` | `size=64` | 52,948.6 ± 709.6 | — | 136,324.5 | — |
| `cModifyId` | `size=8` | 8,905.8 ± 206.0 | — | 20,744.1 | — |
| `cReadId` | `size=512` | 220,865.2 ± 2,677.2 | — | 804,118.8 | — |
| `cReadId` | `size=64` | 27,694.6 ± 413.9 | — | 101,325.3 | — |
| `cReadId` | `size=8` | 4,433.2 ± 67.3 | — | 15,608.0 | — |
| `cReadStreet` | `size=512` | 222,975.2 ± 11,409.1 | — | 804,119.4 | — |
| `cReadStreet` | `size=64` | 27,368.3 ± 242.7 | — | 101,316.8 | — |
| `cReadStreet` | `size=8` | 4,418.9 ± 86.6 | — | 15,608.0 | — |
| `cReplaceId` | `size=512` | 411,017.8 ± 4,585.8 | — | 1,072,872.6 | — |
| `cReplaceId` | `size=64` | 52,836.6 ± 524.6 | — | 136,253.8 | — |
| `cReplaceId` | `size=8` | 8,730.9 ± 74.1 | — | 20,672.1 | — |
| `cSumPrices` | `size=512` | 348,130.6 ± 1,622.7 | — | 1,250,963.0 | — |
| `cSumPrices` | `size=64` | 42,783.7 ± 228.1 | — | 157,585.9 | — |
| `cSumPrices` | `size=8` | 6,417.3 ± 24.5 | — | 22,784.1 | — |
| `jMiss` | `size=512` | 171.7 ± 0.4 | — | 0.0 | — |
| `jMiss` | `size=64` | 171.5 ± 0.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 172.2 ± 8.0 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,846.0 ± 72.4 | — | 41,921.1 | — |
| `jModifyId` | `size=64` | 482.3 ± 21.6 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 108.5 ± 1.9 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.8 ± 1.8 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.1 ± 0.2 | — | 48.0 | — |
| `jReadId` | `size=8` | 36.7 ± 2.0 | — | 64.0 | — |
| `jReadStreet` | `size=512` | 197.7 ± 1.5 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 203.5 ± 9.8 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 201.4 ± 8.1 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 3,752.5 ± 106.1 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 463.6 ± 9.6 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 109.8 ± 3.1 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 86,147.0 ± 1,189.6 | — | 63,665.1 | — |
| `jSumPrices` | `size=64` | 10,425.9 ± 207.9 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,474.1 ± 37.4 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.1 | 4.1 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.6 ± 0.4 | 31.8 ± 0.7 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.1 | 3.6 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,869.8 ± 1,614.4 | — | 43,036.1 | — |
| `Fold_powerEach` | `size=256` | 3,685.2 ± 70.2 | — | 9,240.3 | — |
| `Fold_powerEach` | `size=32` | 460.8 ± 4.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 86.7 ± 2.1 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 54,947.4 ± 691.8 | — | 380,249.0 | — |
| `Modify_multiFocus` | `size=256` | 13,033.3 ± 97.1 | — | 88,443.6 | — |
| `Modify_multiFocus` | `size=32` | 1,572.8 ± 47.4 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 250.6 ± 4.6 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 36,005.6 ± 533.2 | — | 115,201.3 | — |
| `Modify_powerEach` | `size=256` | 8,610.3 ± 151.3 | — | 26,104.6 | — |
| `Modify_powerEach` | `size=32` | 834.1 ± 22.1 | — | 3,224.0 | — |
| `Modify_powerEach` | `size=4` | 202.7 ± 23.0 | — | 856.0 | — |
| `naive_listMap` | `size=1024` | 8,312.3 ± 61.7 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,072.4 ± 19.8 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 253.1 ± 5.0 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 34.3 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,315.1 ± 35.3 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 773.1 ± 27.0 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 68.9 ± 0.6 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 9.0 ± 0.5 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.7 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 167.5 ± 0.9 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.5 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 27.1 ± 0.4 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.2 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.4 ± 0.5 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.2 ± 0.5 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.6 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.1 ± 0.5 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,165.1 ± 31.4 | — | 2,864.0 | — |
| `reuseUse` | `-` | 1,075.3 ± 10.6 | — | 2,648.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.9 ± 0.1 | 22.4 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.6 ± 0.2 | 71.2 ± 0.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.1 ± 0.5 | 117.6 ± 1.3 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.8 ± 0.2 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.2 ± 0.1 | 3.7 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.9 ± 0.3 | 7.6 ± 0.3 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,649.4 ± 1,058.6 | — | 101,536.9 | — |
| `ModifyNames` | `size=64` | 4,725.3 ± 50.8 | — | 13,080.2 | — |
| `ModifyNames` | `size=8` | 658.6 ± 2.8 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 134.8 ± 1.1 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 133.8 ± 1.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 133.9 ± 0.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 39.9 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 39.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 39.0 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,925.3 ± 1,145.1 | — | 382,772.1 | — |
| `monocleModifyNames` | `size=64` | 9,891.7 ± 377.2 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,518.4 ± 13.5 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,001.6 ± 323.9 | — | 169,083.6 | — |
| `monocleModifyStreet` | `size=64` | 7,207.7 ± 40.3 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 1,033.2 ± 6.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,755.0 ± 314.4 | — | 69,789.4 | — |
| `monocleReadStreet` | `size=64` | 4,170.7 ± 23.7 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 513.3 ± 3.3 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,370.6 ± 322.0 | — | 226,301.4 | — |
| `naiveModifyNames` | `size=64` | 8,714.2 ± 433.1 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,208.4 ± 19.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,186.7 ± 280.3 | — | 169,061.8 | — |
| `naiveModifyStreet` | `size=64` | 6,917.4 ± 431.5 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,020.7 ± 3.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,952.8 ± 323.1 | — | 69,789.2 | — |
| `naiveReadStreet` | `size=64` | 4,236.4 ± 51.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 511.9 ± 5.4 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 258,490.0 ± 2,815.0 | — | 609,939.5 | — |
| `Names` | `size=64` | 33,308.3 ± 326.5 | — | 79,299.5 | — |
| `Names` | `size=8` | 4,676.3 ± 189.9 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 280,425.8 ± 3,195.7 | — | 679,460.8 | — |
| `NamesIor` | `size=64` | 36,646.3 ± 397.3 | — | 87,013.7 | — |
| `NamesIor` | `size=8` | 4,739.2 ± 84.4 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,093.7 ± 12.1 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,097.3 ± 12.9 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,114.2 ± 13.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,098.3 ± 11.2 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,090.2 ± 26.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,093.5 ± 6.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 257,237.4 ± 6,863.4 | — | 609,914.5 | — |
| `directNames` | `size=64` | 31,922.9 ± 469.8 | — | 76,692.3 | — |
| `directNames` | `size=8` | 4,308.2 ± 68.3 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,103.9 ± 9.4 | — | 2,736.9 | — |
| `directStreet` | `size=64` | 1,098.4 ± 21.7 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,089.2 ± 15.8 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 267,312.5 ± 9,630.8 | — | 614,010.5 | — |
| `hcursorNames` | `size=64` | 32,524.0 ± 244.9 | — | 77,791.5 | — |
| `hcursorNames` | `size=8` | 4,430.3 ± 106.8 | — | 10,624.1 | — |
| `hcursorStreet` | `size=512` | 1,178.4 ± 12.1 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,189.4 ± 19.3 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,161.7 ± 9.1 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 235,271.1 ± 2,057.8 | — | 1,121,771.4 | — |
| `monocleNames` | `size=64` | 25,851.9 ± 249.2 | — | 132,765.1 | — |
| `monocleNames` | `size=8` | 3,989.3 ± 30.6 | — | 19,498.7 | — |
| `monocleStreet` | `size=512` | 193,842.0 ± 2,594.8 | — | 908,044.9 | — |
| `monocleStreet` | `size=64` | 22,651.1 ± 184.4 | — | 113,799.4 | — |
| `monocleStreet` | `size=8` | 3,443.1 ± 8.7 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 208,057.2 ± 5,732.5 | — | 965,281.1 | — |
| `naiveNames` | `size=64` | 24,550.3 ± 193.8 | — | 120,831.8 | — |
| `naiveNames` | `size=8` | 3,626.7 ± 10.4 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 190,636.5 ± 1,572.3 | — | 908,032.1 | — |
| `naiveStreet` | `size=64` | 22,987.6 ± 425.6 | — | 113,786.1 | — |
| `naiveStreet` | `size=8` | 3,462.6 ± 26.2 | — | 17,050.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,318.1 ± 109.1 | — | 42,027.9 | — |
| `ModifyStreet` | `size=64` | 676.4 ± 19.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 314.7 ± 7.7 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 196.8 ± 1.1 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 201.4 ± 5.2 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 197.9 ± 1.0 | — | 128.0 | — |
| `SumPrices` | `size=512` | 87,398.2 ± 3,615.0 | — | 63,718.7 | — |
| `SumPrices` | `size=64` | 10,297.5 ± 29.8 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,498.9 ± 55.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 168,931.3 ± 2,088.2 | — | 333,606.5 | — |
| `monocleModifyStreet` | `size=64` | 20,539.1 ± 76.5 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,386.5 ± 9.7 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,603.4 ± 644.5 | — | 193,265.8 | — |
| `monocleReadStreet` | `size=64` | 12,202.2 ± 178.6 | — | 24,737.2 | — |
| `monocleReadStreet` | `size=8` | 1,932.4 ± 12.2 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 455,478.8 ± 5,743.0 | — | 1,190,806.3 | — |
| `monocleSumPrices` | `size=64` | 16,685.6 ± 73.8 | — | 47,401.7 | — |
| `monocleSumPrices` | `size=8` | 2,615.3 ± 24.0 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 168,245.3 ± 1,088.9 | — | 333,603.9 | — |
| `naiveModifyStreet` | `size=64` | 20,544.7 ± 65.3 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,381.0 ± 31.3 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 95,668.7 ± 693.1 | — | 193,265.9 | — |
| `naiveReadStreet` | `size=64` | 12,110.9 ± 50.1 | — | 24,737.2 | — |
| `naiveReadStreet` | `size=8` | 1,931.4 ± 15.2 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 107,482.3 ± 19,751.0 | — | 230,164.0 | — |
| `naiveSumPrices` | `size=64` | 12,723.1 ± 44.4 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,014.7 ± 11.3 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 37,391.1 ± 211.4 | — | 454.8 | — |
| `nativeReadStreet` | `size=64` | 4,898.8 ± 72.6 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 817.9 ± 12.0 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 64,416.7 ± 352.9 | — | 86,268.0 | — |
| `nativeSumPrices` | `size=64` | 8,000.0 ± 28.8 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,200.3 ± 6.7 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,855.6 ± 2,027.5 | — | 624,393.4 | — |
| `TransformDeep` | `n=512` | 12,925.2 ± 87.3 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,628.8 ± 62.3 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 145,811.6 ± 3,799.3 | 176,351.4 ± 749.3 | 655,370.1 | 753,744.3 |
| `TransformExpr` | `n=512` | 18,075.6 ± 123.4 | 16,032.2 ± 103.4 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,241.2 ± 11.7 | 2,728.4 ± 26.1 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 106,032.2 ± 7,044.3 | — | 786,581.2 | — |
| `UniverseDeep` | `n=512` | 15,747.1 ± 229.8 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,920.3 ± 16.3 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 103,185.6 ± 2,466.6 | 1,789,712.6 ± 12,646.0 | 786,387.1 | 4,752,517.5 |
| `UniverseExpr` | `n=512` | 15,071.2 ± 194.8 | 175,026.9 ± 6,420.7 | 98,185.5 | 483,202.0 |
| `UniverseExpr` | `n=64` | 1,839.2 ± 4.1 | 16,634.2 ± 568.4 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 232,233.2 ± 1,360.7 | 2,091,656.3 ± 76,042.7 | 786,481.0 | 6,489,064.9 |
| `UniverseJson` | `n=512` | 27,021.9 ± 94.4 | 214,614.8 ± 2,878.0 | 98,186.8 | 699,917.9 |
| `UniverseJson` | `n=64` | 3,294.1 ± 19.0 | 20,907.5 ± 428.2 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 35,062.5 ± 251.3 | — | 163,881.5 | — |
| `visitorTransformDeep` | `n=512` | 4,123.0 ± 34.7 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 441.5 ± 6.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,950.6 ± 1,261.5 | — | 360,473.5 | — |
| `visitorTransformExpr` | `n=512` | 8,398.9 ± 76.2 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,046.1 ± 5.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,480.8 ± 587.3 | — | 196,705.1 | — |
| `visitorUniverseDeep` | `n=512` | 6,877.4 ± 52.7 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 809.3 ± 10.6 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,265.5 ± 411.4 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,826.6 ± 19.2 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 835.6 ± 1.8 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 139,000.6 ± 2,489.3 | — | 294,997.2 | — |
| `visitorUniverseJson` | `n=512` | 13,906.6 ± 69.7 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,170.5 ± 298.7 | — | 4,880.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,358.2 ± 74.9 | — | 41,438.8 | — |
| `Modify_powerEach` | `size=16` | 294.6 ± 1.4 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,418.1 ± 18.5 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 141.7 ± 1.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 58,078.7 ± 837.5 | — | 164,406.6 | — |
| `Modify_powerEach` | `size=64` | 916.2 ± 7.1 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 60,587.1 ± 1,496.7 | — | 279,433.3 | — |
| `monocle_powerEach` | `size=16` | 623.7 ± 11.5 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 22,421.5 ± 614.3 | — | 107,347.5 | — |
| `monocle_powerEach` | `size=4` | 257.5 ± 3.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 186,236.9 ± 1,986.3 | — | 967,855.2 | — |
| `monocle_powerEach` | `size=64` | 2,082.9 ± 12.9 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,636.3 ± 74.4 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,610.1 ± 3.2 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.2 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,909.3 ± 841.9 | — | 114,781.3 | — |
| `naive_powerEach` | `size=64` | 395.5 ± 2.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 64,457.1 ± 1,408.2 | — | 210,613.3 | — |
| `Modify_nested` | `size=16` | 1,575.7 ± 14.9 | — | 4,936.1 | — |
| `Modify_nested` | `size=256` | 19,066.1 ± 265.0 | — | 53,939.9 | — |
| `Modify_nested` | `size=4` | 757.6 ± 12.8 | — | 2,408.0 | — |
| `Modify_nested` | `size=64` | 4,779.9 ± 98.4 | — | 14,760.5 | — |
| `monocle_nested` | `size=1024` | 258,683.0 ± 5,959.2 | — | 1,118,870.9 | — |
| `monocle_nested` | `size=16` | 2,914.4 ± 16.7 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 95,226.2 ± 2,054.2 | — | 430,211.0 | — |
| `monocle_nested` | `size=4` | 1,277.0 ± 31.6 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 9,185.0 ± 135.2 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 21,691.1 ± 1,010.3 | — | 115,071.5 | — |
| `naive_nested` | `size=16` | 390.0 ± 7.7 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,856.3 ± 64.6 | — | 29,019.0 | — |
| `naive_nested` | `size=4` | 134.3 ± 3.1 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,421.8 ± 18.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,650.7 ± 167.0 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,058.2 ± 464.5 | — | 104,802.0 | — |
| `Modify_sparse` | `size=32` | 518.8 ± 5.3 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,179.1 ± 25.4 | — | 24,906.0 | — |
| `Modify_sparse` | `size=8` | 173.9 ± 1.5 | — | 613.3 | — |
| `monocle_sparse` | `size=128` | 3,745.6 ± 30.7 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 99,504.1 ± 751.7 | — | 476,032.3 | — |
| `monocle_sparse` | `size=32` | 1,003.0 ± 20.3 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,939.9 ± 324.1 | — | 156,443.2 | — |
| `monocle_sparse` | `size=8` | 312.3 ± 14.4 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 326.7 ± 1.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,517.6 ± 59.1 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 84.3 ± 0.4 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,293.4 ± 4.3 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.1 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 6.0 ± 5.6 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.4 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.4 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 144,190.5 ± 528.9 | — | 786,297.0 | — |
| `Cata` | `-` | 83,046.3 ± 943.6 | — | 197,568.6 | — |
| `Hylo` | `-` | 86,866.2 ± 1,859.2 | — | 295,848.6 | — |
| `drosteAna` | `-` | 54,395.1 ± 3,792.2 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,738.0 ± 286.2 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,246.3 ± 674.1 | — | 328,640.5 | — |
| `handAna` | `-` | 19,898.5 ± 184.2 | — | 163,816.1 | — |
| `handCata` | `-` | 13,124.5 ± 36.3 | — | 0.1 | — |
| `handHylo` | `-` | 11,576.5 ± 166.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.1 | 2.4 ± 0.1 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.5 ± 0.0 | 26.1 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.1 ± 0.2 | 59.7 ± 1.1 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.1 | 3.4 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,373.2 ± 345.9 | — | 20,224.9 | — |
| `FoldNested` | `size=64` | 601.6 ± 8.7 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 54.3 ± 0.7 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,806.2 ± 20.4 | 31,952.8 ± 450.1 | 12,312.4 | 162,581.1 |
| `FoldPrices` | `size=64` | 352.3 ± 5.2 | 2,298.9 ± 20.3 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 44.5 ± 0.1 | 339.5 ± 18.1 | 216.0 | 2,010.7 |
| `Modify` | `size=512` | 7,845.3 ± 178.1 | 35,681.9 ± 294.8 | 36,897.2 | 176,925.5 |
| `Modify` | `size=64` | 851.7 ± 2.8 | 1,804.4 ± 29.5 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 102.2 ± 0.4 | 288.3 ± 3.1 | 608.0 | 1,936.0 |

