# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `e5bd4f3bea01f2d804dd9e1a6845e6f49156a901` · date: `2026-07-20` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.1 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.1 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.5 ± 0.2 | 23.4 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.4 ± 5.2 | — | 704.0 | — |
| `ModifyCountry` | `-` | 358.2 ± 6.8 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 451.1 ± 6.5 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 174.9 ± 5.2 | — | 498.7 | — |
| `ReadPartner` | `-` | 206.7 ± 4.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 324.0 ± 6.2 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,737.4 ± 44.1 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,659.1 ± 10.7 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,177.9 ± 347.7 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,715.0 ± 16.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,725.9 ± 13.4 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 774.3 ± 8.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 584.5 ± 16.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 405.8 ± 2.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 423.6 ± 2.9 | — | 1,504.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,305.2 ± 3.9 | — | 3,640.0 | — |
| `freshDecodeRecord` | `-` | 1,289.3 ± 17.3 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,510.6 ± 31.8 | — | 9,405.4 | — |
| `ClickToJson` | `-` | 2,988.3 ± 15.3 | — | 4,032.0 | — |
| `WideToAvro` | `-` | 1,043.0 ± 9.3 | — | 6,552.0 | — |
| `WideToJson` | `-` | 717.2 ± 24.5 | — | 1,381.3 | — |
| `naiveClickToAvro` | `-` | 1,768.0 ± 27.5 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,784.8 ± 22.5 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,025.5 ± 10.3 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,942.1 ± 50.3 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 227.3 ± 1.4 | — | 984.0 | — |
| `decode_native` | `-` | 18.5 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 231.4 ± 16.7 | — | 984.0 | — |
| `encode_bridged` | `-` | 258.3 ± 9.5 | — | 1,282.7 | — |
| `encode_native` | `-` | 14.8 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 262.3 ± 2.3 | — | 1,293.3 | — |
| `fieldGet_bridged` | `-` | 98.3 ± 2.7 | — | 437.3 | — |
| `fieldGet_native` | `-` | 98.5 ± 1.3 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 446.7 ± 18.4 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 181.8 ± 2.5 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 38.2 ± 8.5 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.6 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.0 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 41.4 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.2 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 133.4 ± 0.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 62.3 ± 1.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,428.3 ± 13.1 | 4,501.0 ± 47.5 | 14,080.7 | 14,080.7 |
| `FoldMap` | `size=64` | 325.8 ± 0.8 | 306.9 ± 0.6 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.1 | 20.4 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,788.4 ± 19.2 | 2,774.0 ± 7.2 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 349.3 ± 0.5 | 353.3 ± 0.6 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.5 ± 0.3 | 45.4 ± 1.3 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.0 ± 0.0 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.2 ± 0.2 | 26.0 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 408,362.3 ± 4,510.8 | — | 1,073,007.8 | — |
| `cModifyId` | `size=64` | 52,944.8 ± 507.4 | — | 136,394.4 | — |
| `cModifyId` | `size=8` | 8,914.7 ± 55.3 | — | 21,304.1 | — |
| `cReadId` | `size=512` | 220,543.2 ± 3,887.2 | — | 804,182.7 | — |
| `cReadId` | `size=64` | 28,294.4 ± 793.2 | — | 101,398.2 | — |
| `cReadId` | `size=8` | 4,420.8 ± 59.4 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 220,991.6 ± 6,549.7 | — | 804,350.8 | — |
| `cReadStreet` | `size=64` | 27,448.0 ± 519.5 | — | 101,549.2 | — |
| `cReadStreet` | `size=8` | 4,465.4 ± 58.3 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 420,485.8 ± 18,147.1 | — | 1,072,945.4 | — |
| `cReplaceId` | `size=64` | 53,217.0 ± 1,488.5 | — | 136,337.9 | — |
| `cReplaceId` | `size=8` | 8,814.9 ± 45.9 | — | 20,736.1 | — |
| `cSumPrices` | `size=512` | 349,977.6 ± 6,018.0 | — | 1,253,091.5 | — |
| `cSumPrices` | `size=64` | 42,957.2 ± 391.1 | — | 157,859.0 | — |
| `cSumPrices` | `size=8` | 6,448.3 ± 46.2 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 164.4 ± 0.7 | — | 0.0 | — |
| `jMiss` | `size=64` | 163.7 ± 0.5 | — | 0.0 | — |
| `jMiss` | `size=8` | 170.6 ± 9.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,326.6 ± 32.0 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 425.1 ± 3.7 | — | 5,344.0 | — |
| `jModifyId` | `size=8` | 106.4 ± 1.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.6 ± 3.3 | — | 56.0 | — |
| `jReadId` | `size=64` | 36.1 ± 2.3 | — | 56.0 | — |
| `jReadId` | `size=8` | 35.0 ± 0.6 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 180.2 ± 1.5 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 179.2 ± 0.9 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 182.0 ± 4.6 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,304.3 ± 28.7 | — | 41,888.9 | — |
| `jReplaceId` | `size=64` | 415.6 ± 5.9 | — | 5,312.0 | — |
| `jReplaceId` | `size=8` | 106.6 ± 3.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 86,163.4 ± 339.2 | — | 63,665.6 | — |
| `jSumPrices` | `size=64` | 10,746.7 ± 625.7 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,432.0 ± 12.0 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.5 ± 0.2 | 31.3 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.1 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,337.5 ± 878.5 | — | 380,269.4 | — |
| `Modify_multiFocus` | `size=256` | 13,249.1 ± 544.0 | — | 88,416.9 | — |
| `Modify_multiFocus` | `size=32` | 1,541.1 ± 4.4 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 255.2 ± 12.8 | — | 1,533.3 | — |
| `Modify_powerEach` | `size=1024` | 35,797.8 ± 213.0 | — | 119,393.1 | — |
| `Modify_powerEach` | `size=256` | 8,837.9 ± 63.4 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 934.7 ± 120.5 | — | 3,288.0 | — |
| `Modify_powerEach` | `size=4` | 200.7 ± 17.4 | — | 936.0 | — |
| `naive_listMap` | `size=1024` | 8,307.0 ± 39.4 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,065.2 ± 29.8 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 247.2 ± 1.3 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.1 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 67.0 ± 0.4 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 164.8 ± 0.7 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.8 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.3 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.8 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 144.9 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.8 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,242.3 ± 19.2 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,158.4 ± 17.8 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.0 ± 0.3 | 22.4 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.4 ± 0.2 | 71.7 ± 1.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 153.9 ± 2.9 | 115.8 ± 1.3 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.7 ± 0.1 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.1 ± 0.0 | 6.9 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,811.5 ± 1,025.0 | — | 97,656.9 | — |
| `ModifyNames` | `size=64` | 4,678.7 ± 19.0 | — | 12,792.2 | — |
| `ModifyNames` | `size=8` | 658.8 ± 5.9 | — | 2,208.0 | — |
| `ModifyStreet` | `size=512` | 134.5 ± 2.1 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 133.1 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 133.6 ± 1.1 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 40.6 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 41.1 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.7 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,187.1 ± 502.0 | — | 382,772.7 | — |
| `monocleModifyNames` | `size=64` | 9,917.8 ± 455.5 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,530.8 ± 11.9 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 54,889.1 ± 355.4 | — | 169,084.5 | — |
| `monocleModifyStreet` | `size=64` | 6,927.7 ± 472.5 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 1,029.5 ± 7.1 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,638.5 ± 229.9 | — | 69,789.3 | — |
| `monocleReadStreet` | `size=64` | 4,197.1 ± 19.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 521.4 ± 6.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,633.5 ± 704.5 | — | 226,301.5 | — |
| `naiveModifyNames` | `size=64` | 8,753.4 ± 360.6 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,208.6 ± 27.7 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,187.1 ± 467.7 | — | 169,061.8 | — |
| `naiveModifyStreet` | `size=64` | 7,065.2 ± 591.9 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,012.4 ± 5.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,878.7 ± 232.0 | — | 69,789.1 | — |
| `naiveReadStreet` | `size=64` | 4,203.5 ± 8.4 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 518.0 ± 18.6 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 268,970.6 ± 8,259.4 | — | 646,885.0 | — |
| `Names` | `size=64` | 34,378.0 ± 432.5 | — | 82,427.5 | — |
| `Names` | `size=8` | 4,649.4 ± 68.4 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 305,434.2 ± 5,738.0 | — | 724,617.9 | — |
| `NamesIor` | `size=64` | 37,950.1 ± 806.1 | — | 90,650.3 | — |
| `NamesIor` | `size=8` | 4,955.6 ± 58.9 | — | 11,960.1 | — |
| `Street` | `size=512` | 1,186.8 ± 18.9 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,165.0 ± 13.3 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,158.8 ± 18.6 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,180.8 ± 36.1 | — | 2,976.9 | — |
| `StreetIor` | `size=64` | 1,150.9 ± 25.0 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,170.1 ± 5.9 | — | 2,984.0 | — |
| `directNames` | `size=512` | 264,274.3 ± 5,893.9 | — | 614,008.1 | — |
| `directNames` | `size=64` | 32,353.1 ± 609.5 | — | 77,197.6 | — |
| `directNames` | `size=8` | 4,380.0 ± 81.2 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,082.0 ± 13.7 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,075.4 ± 17.6 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,078.7 ± 36.7 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 272,053.2 ± 8,977.2 | — | 614,040.9 | — |
| `hcursorNames` | `size=64` | 32,404.3 ± 338.5 | — | 77,791.6 | — |
| `hcursorNames` | `size=8` | 4,448.9 ± 40.1 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,170.3 ± 13.5 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,185.4 ± 14.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,220.6 ± 15.8 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 235,533.0 ± 2,473.1 | — | 1,121,776.9 | — |
| `monocleNames` | `size=64` | 25,942.0 ± 161.7 | — | 132,759.8 | — |
| `monocleNames` | `size=8` | 4,019.6 ± 97.0 | — | 19,482.7 | — |
| `monocleStreet` | `size=512` | 188,105.5 ± 1,113.0 | — | 908,038.4 | — |
| `monocleStreet` | `size=64` | 22,790.7 ± 175.5 | — | 113,804.7 | — |
| `monocleStreet` | `size=8` | 3,466.9 ± 39.5 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 207,637.8 ± 5,947.2 | — | 965,275.5 | — |
| `naiveNames` | `size=64` | 24,357.9 ± 226.9 | — | 120,853.0 | — |
| `naiveNames` | `size=8` | 3,652.3 ± 42.0 | — | 17,829.4 | — |
| `naiveStreet` | `size=512` | 190,170.2 ± 2,446.0 | — | 908,031.8 | — |
| `naiveStreet` | `size=64` | 22,833.4 ± 270.5 | — | 113,780.7 | — |
| `naiveStreet` | `size=8` | 3,446.6 ± 61.7 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,680.8 ± 48.7 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 594.3 ± 2.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 280.9 ± 7.2 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 179.6 ± 1.0 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 183.6 ± 2.8 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 181.4 ± 1.1 | — | 128.0 | — |
| `SumPrices` | `size=512` | 86,487.0 ± 563.4 | — | 63,719.6 | — |
| `SumPrices` | `size=64` | 10,399.9 ± 160.8 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,444.9 ± 36.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 166,498.3 ± 1,178.0 | — | 333,618.4 | — |
| `monocleModifyStreet` | `size=64` | 20,833.4 ± 141.9 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,402.0 ± 7.2 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 96,338.4 ± 5,304.9 | — | 193,266.5 | — |
| `monocleReadStreet` | `size=64` | 12,228.6 ± 215.0 | — | 24,737.2 | — |
| `monocleReadStreet` | `size=8` | 1,933.1 ± 8.5 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 445,866.5 ± 11,146.9 | — | 1,190,797.8 | — |
| `monocleSumPrices` | `size=64` | 16,995.1 ± 285.2 | — | 47,417.7 | — |
| `monocleSumPrices` | `size=8` | 2,665.7 ± 65.4 | — | 6,680.1 | — |
| `naiveModifyStreet` | `size=512` | 167,373.8 ± 1,622.6 | — | 333,569.2 | — |
| `naiveModifyStreet` | `size=64` | 20,575.5 ± 160.6 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,420.1 ± 24.9 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 97,226.9 ± 2,248.5 | — | 193,267.2 | — |
| `naiveReadStreet` | `size=64` | 13,122.6 ± 822.7 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,937.2 ± 25.8 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 99,718.8 ± 371.0 | — | 230,157.4 | — |
| `naiveSumPrices` | `size=64` | 12,745.3 ± 77.4 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,021.3 ± 14.1 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 38,543.9 ± 1,706.1 | — | 455.7 | — |
| `nativeReadStreet` | `size=64` | 4,760.8 ± 14.0 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 803.5 ± 3.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 64,988.0 ± 367.0 | — | 86,269.0 | — |
| `nativeSumPrices` | `size=64` | 8,006.4 ± 42.7 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,221.4 ± 38.7 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 132,278.4 ± 656.7 | — | 624,392.3 | — |
| `TransformDeep` | `n=512` | 13,058.1 ± 75.8 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,613.6 ± 12.8 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 147,077.6 ± 302.4 | 176,302.4 ± 863.1 | 655,371.0 | 753,744.3 |
| `TransformExpr` | `n=512` | 18,247.6 ± 146.1 | 15,989.0 ± 64.1 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,247.1 ± 13.2 | 2,706.7 ± 7.8 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 99,525.5 ± 1,476.4 | — | 786,576.4 | — |
| `UniverseDeep` | `n=512` | 15,342.6 ± 93.4 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,865.3 ± 6.4 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 97,817.8 ± 846.1 | 1,780,073.3 ± 3,532.2 | 786,383.2 | 4,752,510.5 |
| `UniverseExpr` | `n=512` | 14,487.3 ± 87.0 | 174,895.4 ± 6,633.8 | 98,185.5 | 483,201.8 |
| `UniverseExpr` | `n=64` | 1,752.1 ± 8.0 | 16,718.3 ± 610.1 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 181,382.5 ± 997.8 | 1,950,851.9 ± 30,590.9 | 786,444.0 | 6,488,962.3 |
| `UniverseJson` | `n=512` | 20,304.6 ± 86.4 | 212,417.3 ± 3,093.1 | 98,186.1 | 699,917.9 |
| `UniverseJson` | `n=64` | 2,461.8 ± 4.2 | 21,379.9 ± 495.6 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,898.1 ± 212.2 | — | 163,881.4 | — |
| `visitorTransformDeep` | `n=512` | 4,100.4 ± 19.3 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 431.9 ± 1.1 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 66,800.8 ± 369.6 | — | 360,472.6 | — |
| `visitorTransformExpr` | `n=512` | 8,299.6 ± 26.2 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,045.6 ± 16.3 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,453.4 ± 863.0 | — | 196,705.1 | — |
| `visitorUniverseDeep` | `n=512` | 6,866.8 ± 28.2 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 802.4 ± 3.0 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,045.0 ± 368.4 | — | 196,656.1 | — |
| `visitorUniverseExpr` | `n=512` | 6,837.0 ± 22.0 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 837.7 ± 2.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 154,602.8 ± 19,520.6 | — | 313,728.5 | — |
| `visitorUniverseJson` | `n=512` | 14,007.8 ± 113.4 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,332.5 ± 281.1 | — | 5,216.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,861.3 ± 80.7 | — | 41,438.5 | — |
| `Modify_powerEach` | `size=16` | 298.1 ± 4.3 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,329.3 ± 27.1 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 140.3 ± 2.4 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 56,839.2 ± 613.5 | — | 164,431.7 | — |
| `Modify_powerEach` | `size=64` | 903.7 ± 6.3 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 59,157.8 ± 688.9 | — | 279,433.7 | — |
| `monocle_powerEach` | `size=16` | 610.1 ± 34.3 | — | 3,725.3 | — |
| `monocle_powerEach` | `size=256` | 21,845.3 ± 168.1 | — | 107,339.5 | — |
| `monocle_powerEach` | `size=4` | 254.6 ± 4.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 185,233.1 ± 3,394.1 | — | 967,871.8 | — |
| `monocle_powerEach` | `size=64` | 2,116.6 ± 38.1 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,526.6 ± 8.1 | — | 28,730.8 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 0.4 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,611.6 ± 2.4 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.0 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,402.8 ± 288.4 | — | 114,782.9 | — |
| `naive_powerEach` | `size=64` | 395.6 ± 3.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 65,291.9 ± 1,999.2 | — | 210,968.0 | — |
| `Modify_nested` | `size=16` | 1,700.2 ± 15.7 | — | 5,296.1 | — |
| `Modify_nested` | `size=256` | 16,769.2 ± 593.1 | — | 54,197.1 | — |
| `Modify_nested` | `size=4` | 822.5 ± 4.7 | — | 2,720.0 | — |
| `Modify_nested` | `size=64` | 4,215.7 ± 229.1 | — | 15,120.5 | — |
| `monocle_nested` | `size=1024` | 251,863.6 ± 1,277.8 | — | 1,118,883.3 | — |
| `monocle_nested` | `size=16` | 2,908.3 ± 45.4 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 95,414.0 ± 2,066.2 | — | 430,213.3 | — |
| `monocle_nested` | `size=4` | 1,361.5 ± 59.2 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 9,013.1 ± 39.2 | — | 58,907.7 | — |
| `naive_nested` | `size=1024` | 21,604.5 ± 954.5 | — | 115,073.5 | — |
| `naive_nested` | `size=16` | 388.4 ± 5.4 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,813.4 ± 29.2 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 134.2 ± 3.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,411.7 ± 13.7 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,656.9 ± 171.0 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,619.8 ± 581.6 | — | 104,810.9 | — |
| `Modify_sparse` | `size=32` | 516.3 ± 5.0 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,184.0 ± 41.1 | — | 24,900.6 | — |
| `Modify_sparse` | `size=8` | 177.7 ± 2.5 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,743.7 ± 48.5 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 99,361.1 ± 1,158.7 | — | 476,033.3 | — |
| `monocle_sparse` | `size=32` | 994.6 ± 26.7 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,896.3 ± 270.9 | — | 156,443.0 | — |
| `monocle_sparse` | `size=8` | 312.8 ± 8.1 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 326.2 ± 0.8 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,448.5 ± 42.4 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,291.0 ± 6.8 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.2 ± 0.2 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 36.9 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.7 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 142,914.5 ± 696.5 | — | 786,297.0 | — |
| `Cata` | `-` | 85,191.4 ± 624.2 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,371.4 ± 1,390.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 52,789.1 ± 3,793.4 | — | 327,632.4 | — |
| `drosteCata` | `-` | 45,171.6 ± 329.2 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,590.7 ± 452.2 | — | 328,640.5 | — |
| `handAna` | `-` | 19,552.0 ± 116.2 | — | 163,816.1 | — |
| `handCata` | `-` | 13,117.4 ± 45.7 | — | 0.1 | — |
| `handHylo` | `-` | 11,204.3 ± 331.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 1.1 | 25.9 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 25.9 ± 0.1 | 59.2 ± 0.5 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.0 ± 0.0 | 3.0 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,085.1 ± 45.1 | 36,163.8 ± 926.4 | 39,001.1 | 176,924.6 |
| `Modify` | `size=64` | 958.8 ± 3.0 | 1,763.3 ± 6.9 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 109.8 ± 0.9 | 287.9 ± 1.8 | 728.0 | 1,936.0 |

