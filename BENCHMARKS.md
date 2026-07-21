# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `47fac97486070d2335a551e1bb620d4a9631e6f7` · date: `2026-07-21` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 11.7 ± 0.0 | 8.0 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.5 ± 8.6 | 18.6 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 125.2 ± 7.4 | — | 714.7 | — |
| `ModifyCountry` | `-` | 257.1 ± 3.7 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 298.5 ± 4.5 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 142.2 ± 2.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 167.9 ± 6.8 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 249.7 ± 1.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,094.4 ± 4.1 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,073.1 ± 3.7 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 3,301.6 ± 110.0 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,287.0 ± 8.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,310.7 ± 8.5 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 573.6 ± 2.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 422.2 ± 4.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 320.1 ± 6.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 335.7 ± 1.0 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,016.8 ± 6.8 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,003.0 ± 14.6 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,723.1 ± 31.9 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,295.3 ± 13.9 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 620.7 ± 21.4 | — | 6,552.0 | — |
| `WideToJson` | `-` | 461.1 ± 1.0 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,298.1 ± 25.2 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,205.9 ± 47.7 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 747.6 ± 2.7 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,541.2 ± 24.3 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 174.4 ± 0.2 | — | 984.0 | — |
| `decode_native` | `-` | 15.2 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 170.1 ± 0.3 | — | 984.0 | — |
| `encode_bridged` | `-` | 185.6 ± 3.9 | — | 1,282.7 | — |
| `encode_native` | `-` | 12.3 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 179.3 ± 0.8 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 80.5 ± 1.5 | — | 437.3 | — |
| `fieldGet_native` | `-` | 82.5 ± 0.8 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 304.9 ± 3.2 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 141.6 ± 2.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 16.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 17.7 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.4 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.5 ± 0.9 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 26.1 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 30.0 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 4.2 ± 0.2 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.1 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.8 ± 0.8 | — | 72.0 | — |
| `buildLens3` | `-` | 17.3 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 33.8 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 17.5 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.7 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 37.8 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 104.9 ± 0.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 49.7 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,900.6 ± 2.8 | 3,667.0 ± 3.3 | 14,080.6 | 14,080.6 |
| `FoldMap` | `size=64` | 251.9 ± 0.4 | 242.6 ± 0.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 15.9 ± 0.1 | 17.0 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,389.0 ± 25.7 | 2,392.1 ± 11.5 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 288.6 ± 0.4 | 288.3 ± 0.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 37.3 ± 0.1 | 37.3 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
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
| `Get_3` | `-` | 14.2 ± 0.0 | 6.8 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 27.0 ± 0.1 | 21.4 ± 0.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.9 ± 0.0 | 3.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 329,500.1 ± 3,156.7 | — | 1,072,999.6 | — |
| `cModifyId` | `size=64` | 43,122.0 ± 779.7 | — | 136,409.0 | — |
| `cModifyId` | `size=8` | 7,347.1 ± 16.1 | — | 20,832.1 | — |
| `cReadId` | `size=512` | 168,917.7 ± 333.7 | — | 804,168.0 | — |
| `cReadId` | `size=64` | 21,584.6 ± 214.5 | — | 101,368.6 | — |
| `cReadId` | `size=8` | 3,441.0 ± 9.6 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 168,667.7 ± 453.8 | — | 804,336.0 | — |
| `cReadStreet` | `size=64` | 21,511.9 ± 62.5 | — | 101,552.6 | — |
| `cReadStreet` | `size=8` | 3,465.2 ± 5.3 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 328,635.5 ± 3,324.6 | — | 1,072,917.4 | — |
| `cReplaceId` | `size=64` | 42,034.7 ± 223.7 | — | 136,323.2 | — |
| `cReplaceId` | `size=8` | 7,431.3 ± 11.3 | — | 21,480.1 | — |
| `cSumPrices` | `size=512` | 274,653.7 ± 1,123.0 | — | 1,253,062.1 | — |
| `cSumPrices` | `size=64` | 34,230.4 ± 143.2 | — | 158,172.5 | — |
| `cSumPrices` | `size=8` | 5,087.9 ± 90.5 | — | 22,920.1 | — |
| `jMiss` | `size=512` | 149.5 ± 0.2 | — | 0.0 | — |
| `jMiss` | `size=64` | 149.8 ± 0.4 | — | 0.0 | — |
| `jMiss` | `size=8` | 150.1 ± 1.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,456.3 ± 23.5 | — | 41,920.7 | — |
| `jModifyId` | `size=64` | 284.2 ± 1.7 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 81.4 ± 1.0 | — | 992.0 | — |
| `jReadId` | `size=512` | 28.8 ± 1.5 | — | 56.0 | — |
| `jReadId` | `size=64` | 27.6 ± 0.2 | — | 48.0 | — |
| `jReadId` | `size=8` | 28.9 ± 1.2 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 164.5 ± 2.6 | — | 136.0 | — |
| `jReadStreet` | `size=64` | 163.6 ± 1.1 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 162.8 ± 1.7 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,413.6 ± 7.5 | — | 41,888.7 | — |
| `jReplaceId` | `size=64` | 277.9 ± 4.3 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 79.6 ± 2.5 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 68,372.2 ± 188.7 | — | 63,673.7 | — |
| `jSumPrices` | `size=64` | 8,487.3 ± 42.1 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,141.9 ± 12.1 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.1 ± 0.0 | 3.2 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 30.2 ± 0.1 | 25.5 ± 0.0 | 152.0 | 176.0 |
| `Replace` | `-` | 2.8 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 41,455.3 ± 182.1 | — | 380,250.4 | — |
| `Modify_multiFocus` | `size=256` | 10,276.9 ± 26.4 | — | 88,400.7 | — |
| `Modify_multiFocus` | `size=32` | 1,219.2 ± 4.7 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 176.4 ± 1.8 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 29,448.7 ± 63.3 | — | 119,391.4 | — |
| `Modify_powerEach` | `size=256` | 7,172.8 ± 37.3 | — | 27,168.5 | — |
| `Modify_powerEach` | `size=32` | 881.1 ± 8.7 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 161.6 ± 15.1 | — | 920.0 | — |
| `naive_listMap` | `size=1024` | 7,007.8 ± 23.2 | — | 65,577.8 | — |
| `naive_listMap` | `size=256` | 1,761.1 ± 24.7 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 214.4 ± 0.7 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 31.2 ± 0.3 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 53.5 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 140.2 ± 1.1 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 12.8 ± 0.3 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 20.5 ± 0.0 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 32.4 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 6.3 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 11.5 ± 0.0 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 133.4 ± 0.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 38.3 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 851.6 ± 16.7 | — | 3,152.0 | — |
| `reuseUse` | `-` | 809.9 ± 10.6 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 18.5 ± 0.1 | 17.5 ± 0.0 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 49.7 ± 0.1 | 51.5 ± 0.1 | 160.0 | 304.0 |
| `Modify_6` | `-` | 117.2 ± 0.1 | 85.7 ± 0.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.4 ± 0.0 | 16.3 ± 0.5 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.3 ± 0.0 | 2.9 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.1 ± 0.0 | 5.8 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 31,597.1 ± 1,360.2 | — | 97,439.6 | — |
| `ModifyNames` | `size=64` | 3,819.2 ± 13.2 | — | 12,568.1 | — |
| `ModifyNames` | `size=8` | 523.1 ± 3.5 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 110.7 ± 0.2 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 113.0 ± 3.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.7 ± 0.1 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 33.3 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 32.8 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 33.0 ± 0.4 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 81,302.9 ± 329.8 | — | 382,784.0 | — |
| `monocleModifyNames` | `size=64` | 7,568.7 ± 30.0 | — | 39,832.3 | — |
| `monocleModifyNames` | `size=8` | 1,117.9 ± 11.5 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 46,335.8 ± 984.0 | — | 169,080.6 | — |
| `monocleModifyStreet` | `size=64` | 5,863.5 ± 13.7 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 760.9 ± 7.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 28,390.6 ± 205.1 | — | 69,784.7 | — |
| `monocleReadStreet` | `size=64` | 3,652.0 ± 28.7 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 422.3 ± 1.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 57,139.6 ± 180.9 | — | 226,294.8 | — |
| `naiveModifyNames` | `size=64` | 7,082.7 ± 376.8 | — | 27,928.2 | — |
| `naiveModifyNames` | `size=8` | 901.7 ± 7.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,908.1 ± 502.6 | — | 169,056.7 | — |
| `naiveModifyStreet` | `size=64` | 5,585.4 ± 397.0 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 763.3 ± 5.4 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 28,341.3 ± 413.0 | — | 69,784.4 | — |
| `naiveReadStreet` | `size=64` | 3,680.6 ± 5.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 423.4 ± 3.6 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 183,491.6 ± 3,946.0 | — | 646,800.5 | — |
| `Names` | `size=64` | 23,155.7 ± 232.8 | — | 82,938.4 | — |
| `Names` | `size=8` | 3,187.3 ± 81.4 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 204,461.8 ± 6,745.0 | — | 724,537.0 | — |
| `NamesIor` | `size=64` | 24,671.8 ± 371.7 | — | 91,178.6 | — |
| `NamesIor` | `size=8` | 3,218.9 ± 62.6 | — | 12,000.1 | — |
| `Street` | `size=512` | 808.0 ± 9.7 | — | 2,968.6 | — |
| `Street` | `size=64` | 804.3 ± 11.5 | — | 2,968.1 | — |
| `Street` | `size=8` | 801.1 ± 7.6 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 826.6 ± 13.5 | — | 2,976.6 | — |
| `StreetIor` | `size=64` | 807.0 ± 4.3 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 805.2 ± 8.2 | — | 2,976.0 | — |
| `directNames` | `size=512` | 178,173.7 ± 5,140.1 | — | 605,748.3 | — |
| `directNames` | `size=64` | 22,707.0 ± 378.6 | — | 77,709.1 | — |
| `directNames` | `size=8` | 3,083.2 ± 23.0 | — | 10,698.7 | — |
| `directStreet` | `size=512` | 771.2 ± 6.5 | — | 2,744.6 | — |
| `directStreet` | `size=64` | 824.0 ± 75.4 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 772.9 ± 11.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 185,507.9 ± 1,692.9 | — | 613,946.1 | — |
| `hcursorNames` | `size=64` | 22,861.5 ± 435.4 | — | 77,778.4 | — |
| `hcursorNames` | `size=8` | 3,084.0 ± 57.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 815.8 ± 6.4 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 813.4 ± 3.9 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 808.0 ± 4.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 186,284.3 ± 5,204.6 | — | 1,121,743.8 | — |
| `monocleNames` | `size=64` | 20,005.9 ± 132.7 | — | 132,756.5 | — |
| `monocleNames` | `size=8` | 3,010.3 ± 65.7 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 148,404.8 ± 5,140.7 | — | 908,009.0 | — |
| `monocleStreet` | `size=64` | 17,412.6 ± 25.1 | — | 113,793.6 | — |
| `monocleStreet` | `size=8` | 2,541.5 ± 27.5 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 165,519.4 ± 11,169.3 | — | 965,252.5 | — |
| `naiveNames` | `size=64` | 18,908.2 ± 128.8 | — | 120,825.7 | — |
| `naiveNames` | `size=8` | 2,672.9 ± 13.3 | — | 17,818.7 | — |
| `naiveStreet` | `size=512` | 152,297.7 ± 3,405.9 | — | 908,006.3 | — |
| `naiveStreet` | `size=64` | 17,539.3 ± 236.0 | — | 113,785.6 | — |
| `naiveStreet` | `size=8` | 2,508.7 ± 13.3 | — | 17,050.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 2,681.7 ± 13.6 | — | 42,026.4 | — |
| `ModifyStreet` | `size=64` | 417.3 ± 3.7 | — | 5,440.0 | — |
| `ModifyStreet` | `size=8` | 232.4 ± 0.7 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 165.5 ± 1.4 | — | 128.1 | — |
| `ReadStreet` | `size=64` | 164.5 ± 2.1 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 164.0 ± 0.4 | — | 114.7 | — |
| `SumPrices` | `size=512` | 69,423.2 ± 378.7 | — | 63,717.1 | — |
| `SumPrices` | `size=64` | 8,513.5 ± 81.8 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,141.8 ± 9.8 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 134,146.8 ± 399.7 | — | 333,550.0 | — |
| `monocleModifyStreet` | `size=64` | 16,779.5 ± 33.6 | — | 30,113.7 | — |
| `monocleModifyStreet` | `size=8` | 2,728.4 ± 21.0 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 76,575.1 ± 477.0 | — | 193,249.5 | — |
| `monocleReadStreet` | `size=64` | 9,674.2 ± 105.4 | — | 24,737.0 | — |
| `monocleReadStreet` | `size=8` | 1,472.3 ± 5.1 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 373,319.5 ± 2,456.6 | — | 1,190,735.5 | — |
| `monocleSumPrices` | `size=64` | 13,218.3 ± 70.6 | — | 47,417.4 | — |
| `monocleSumPrices` | `size=8` | 2,046.3 ± 62.9 | — | 6,680.0 | — |
| `naiveModifyStreet` | `size=512` | 134,730.5 ± 958.8 | — | 333,541.3 | — |
| `naiveModifyStreet` | `size=64` | 16,793.7 ± 66.7 | — | 30,089.7 | — |
| `naiveModifyStreet` | `size=8` | 2,720.2 ± 13.9 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 76,826.2 ± 583.7 | — | 193,249.8 | — |
| `naiveReadStreet` | `size=64` | 9,649.2 ± 14.7 | — | 24,737.0 | — |
| `naiveReadStreet` | `size=8` | 1,472.5 ± 15.3 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 80,846.6 ± 724.6 | — | 230,141.2 | — |
| `naiveSumPrices` | `size=64` | 10,142.0 ± 16.4 | — | 29,369.0 | — |
| `naiveSumPrices` | `size=8` | 1,533.4 ± 4.6 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 32,650.3 ± 1,091.6 | — | 451.1 | — |
| `nativeReadStreet` | `size=64` | 4,065.0 ± 91.4 | — | 424.4 | — |
| `nativeReadStreet` | `size=8` | 664.4 ± 9.8 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 53,268.8 ± 290.5 | — | 86,253.7 | — |
| `nativeSumPrices` | `size=64` | 6,576.3 ± 9.1 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 966.5 ± 7.3 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 106,396.4 ± 319.3 | — | 624,373.4 | — |
| `TransformDeep` | `n=512` | 10,634.5 ± 60.1 | — | 57,361.1 | — |
| `TransformDeep` | `n=64` | 1,239.0 ± 3.6 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 117,688.5 ± 1,548.4 | 143,824.3 ± 1,274.0 | 655,349.7 | 753,720.7 |
| `TransformExpr` | `n=512` | 14,693.9 ± 205.0 | 13,208.7 ± 19.1 | 81,825.5 | 69,585.3 |
| `TransformExpr` | `n=64` | 1,794.5 ± 6.5 | 1,900.1 ± 464.2 | 10,144.0 | 10,368.0 |
| `UniverseDeep` | `n=4096` | 102,055.0 ± 756.4 | — | 786,578.3 | — |
| `UniverseDeep` | `n=512` | 12,503.0 ± 25.7 | — | 98,377.3 | — |
| `UniverseDeep` | `n=64` | 1,495.2 ± 2.0 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 95,450.2 ± 1,411.2 | 1,373,979.7 ± 57,299.4 | 786,381.5 | 4,752,215.5 |
| `UniverseExpr` | `n=512` | 12,031.7 ± 13.3 | 140,298.9 ± 2,637.6 | 98,185.2 | 483,198.3 |
| `UniverseExpr` | `n=64` | 1,457.3 ± 5.7 | 13,077.6 ± 384.2 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 149,588.4 ± 878.4 | 1,549,901.8 ± 57,903.1 | 786,420.8 | 6,488,671.1 |
| `UniverseJson` | `n=512` | 16,829.8 ± 162.5 | 174,994.8 ± 20,579.7 | 98,185.7 | 699,913.9 |
| `UniverseJson` | `n=64` | 1,980.4 ± 18.7 | 15,926.5 ± 110.9 | 12,168.0 | 73,208.3 |
| `visitorTransformDeep` | `n=4096` | 34,734.4 ± 136.8 | — | 163,881.3 | — |
| `visitorTransformDeep` | `n=512` | 3,238.9 ± 111.8 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 352.3 ± 1.9 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 56,508.0 ± 231.2 | — | 360,465.1 | — |
| `visitorTransformExpr` | `n=512` | 7,050.8 ± 46.3 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 869.8 ± 2.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 46,011.2 ± 945.8 | — | 196,697.5 | — |
| `visitorUniverseDeep` | `n=512` | 5,673.2 ± 107.3 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 648.5 ± 5.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 42,967.4 ± 117.1 | — | 196,647.3 | — |
| `visitorUniverseExpr` | `n=512` | 5,378.3 ± 12.2 | — | 24,584.5 | — |
| `visitorUniverseExpr` | `n=64` | 638.1 ± 1.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 101,234.5 ± 2,361.3 | — | 294,969.7 | — |
| `visitorUniverseJson` | `n=512` | 11,697.9 ± 999.8 | — | 38,209.2 | — |
| `visitorUniverseJson` | `n=64` | 1,548.8 ± 20.0 | — | 4,592.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 11,363.3 ± 29.4 | — | 41,437.4 | — |
| `Modify_powerEach` | `size=16` | 231.6 ± 3.7 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 2,749.7 ± 8.6 | — | 10,712.4 | — |
| `Modify_powerEach` | `size=4` | 103.8 ± 3.6 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 45,278.7 ± 229.7 | — | 164,411.4 | — |
| `Modify_powerEach` | `size=64` | 719.7 ± 1.3 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 46,147.9 ± 2,054.5 | — | 279,424.2 | — |
| `monocle_powerEach` | `size=16` | 434.9 ± 21.3 | — | 3,725.3 | — |
| `monocle_powerEach` | `size=256` | 16,843.2 ± 48.7 | — | 107,338.7 | — |
| `monocle_powerEach` | `size=4` | 179.4 ± 0.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 144,499.7 ± 3,553.1 | — | 967,801.5 | — |
| `monocle_powerEach` | `size=64` | 1,665.6 ± 3.5 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 4,536.9 ± 19.1 | — | 28,730.3 | — |
| `naive_powerEach` | `size=16` | 86.6 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,311.1 ± 1.8 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 22.3 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 17,413.1 ± 1,476.8 | — | 114,774.1 | — |
| `naive_powerEach` | `size=64` | 330.3 ± 3.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 52,006.6 ± 1,471.0 | — | 210,938.8 | — |
| `Modify_nested` | `size=16` | 1,245.6 ± 23.8 | — | 5,208.0 | — |
| `Modify_nested` | `size=256` | 14,279.9 ± 341.0 | — | 54,259.1 | — |
| `Modify_nested` | `size=4` | 589.2 ± 6.3 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 3,350.0 ± 93.6 | — | 15,019.1 | — |
| `monocle_nested` | `size=1024` | 193,613.0 ± 4,784.5 | — | 1,118,771.5 | — |
| `monocle_nested` | `size=16` | 1,991.4 ± 44.6 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 71,152.4 ± 777.8 | — | 430,205.3 | — |
| `monocle_nested` | `size=4` | 936.6 ± 103.1 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 6,931.3 ± 52.1 | — | 58,912.8 | — |
| `naive_nested` | `size=1024` | 17,646.0 ± 139.5 | — | 115,065.9 | — |
| `naive_nested` | `size=16` | 317.7 ± 5.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,991.6 ± 29.2 | — | 29,018.6 | — |
| `naive_nested` | `size=4` | 113.8 ± 4.2 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,140.9 ± 14.0 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,277.4 ± 29.3 | — | 4,933.4 | — |
| `Modify_sparse` | `size=2048` | 23,674.5 ± 310.6 | — | 104,795.3 | — |
| `Modify_sparse` | `size=32` | 424.2 ± 4.9 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 5,893.5 ± 54.3 | — | 24,903.0 | — |
| `Modify_sparse` | `size=8` | 134.8 ± 0.6 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,073.1 ± 50.5 | — | 24,717.5 | — |
| `monocle_sparse` | `size=2048` | 74,257.1 ± 748.5 | — | 476,022.4 | — |
| `monocle_sparse` | `size=32` | 779.1 ± 34.1 | — | 6,349.3 | — |
| `monocle_sparse` | `size=512` | 24,379.1 ± 107.8 | — | 156,437.7 | — |
| `monocle_sparse` | `size=8` | 210.6 ± 0.4 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 250.6 ± 0.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,330.9 ± 21.1 | — | 24,611.4 | — |
| `naive_sparse` | `size=32` | 65.0 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,020.0 ± 2.5 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 20.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.9 ± 0.0 | 2.1 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.8 ± 0.0 | 1.9 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.8 ± 0.0 | 1.9 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 16.5 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 29.1 ± 0.0 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 5.3 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 8.9 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 94,245.8 ± 714.3 | — | 786,296.7 | — |
| `Cata` | `-` | 67,607.8 ± 887.2 | — | 197,568.5 | — |
| `Hylo` | `-` | 68,188.0 ± 536.2 | — | 295,848.5 | — |
| `drosteAna` | `-` | 37,997.7 ± 595.2 | — | 327,632.3 | — |
| `drosteCata` | `-` | 36,277.2 ± 61.3 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 49,299.8 ± 706.9 | — | 328,640.3 | — |
| `handAna` | `-` | 17,275.8 ± 520.4 | — | 163,816.1 | — |
| `handCata` | `-` | 10,217.1 ± 250.4 | — | 0.1 | — |
| `handHylo` | `-` | 9,173.5 ± 347.4 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.8 ± 0.0 | 1.9 ± 0.1 | 24.0 | 24.0 |
| `Modify_3` | `-` | 9.6 ± 0.1 | 20.4 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 20.3 ± 0.0 | 41.2 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 6,351.4 ± 62.9 | 27,757.6 ± 110.8 | 38,960.8 | 176,923.5 |
| `Modify` | `size=64` | 724.2 ± 2.0 | 1,406.9 ± 5.0 | 4,864.0 | 14,448.0 |
| `Modify` | `size=8` | 92.7 ± 1.2 | 187.7 ± 0.6 | 688.0 | 1,936.0 |

