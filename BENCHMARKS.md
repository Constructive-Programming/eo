# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `ffe5116b71e5c093d6ab2cc86ed0473b9f090017` · date: `2026-07-18` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 1.1 ± 0.2 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.8 ± 1.5 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 32.6 ± 2.0 | 23.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 174.0 ± 4.2 | — | 704.0 | — |
| `ModifyCountry` | `-` | 363.1 ± 13.6 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 463.5 ± 23.3 | — | 3,269.3 | — |
| `ReadCountry` | `-` | 168.6 ± 7.5 | — | 520.0 | — |
| `ReadPartner` | `-` | 209.3 ± 6.2 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 320.5 ± 2.9 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,719.2 ± 42.6 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,646.0 ± 15.6 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,223.5 ± 56.2 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,699.9 ± 20.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,724.5 ± 5.9 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 775.6 ± 17.6 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 592.0 ± 6.7 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 402.6 ± 0.9 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 420.3 ± 1.4 | — | 1,504.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,320.3 ± 7.3 | — | 3,640.0 | — |
| `freshDecodeRecord` | `-` | 1,294.5 ± 13.7 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,516.5 ± 43.8 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,988.4 ± 43.7 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 1,022.0 ± 8.9 | — | 6,536.0 | — |
| `WideToJson` | `-` | 739.8 ± 16.3 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,753.8 ± 11.8 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,777.2 ± 13.2 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,033.6 ± 5.9 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,933.2 ± 44.7 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 227.4 ± 0.7 | — | 984.0 | — |
| `decode_native` | `-` | 18.5 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 222.2 ± 3.0 | — | 984.0 | — |
| `encode_bridged` | `-` | 259.2 ± 7.5 | — | 1,288.0 | — |
| `encode_native` | `-` | 14.7 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 258.4 ± 6.3 | — | 1,282.7 | — |
| `fieldGet_bridged` | `-` | 97.7 ± 1.1 | — | 432.0 | — |
| `fieldGet_native` | `-` | 96.8 ± 0.4 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 448.4 ± 3.0 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 175.6 ± 2.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.1 | — | 0.0 | — |
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
| `modifyDeepCap` | `-` | 32.8 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.6 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.4 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.0 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.4 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 41.5 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.2 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 134.4 ± 0.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.7 ± 0.1 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,663.3 ± 196.8 | 4,490.8 ± 22.0 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 326.5 ± 1.5 | 307.4 ± 0.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.0 | 20.3 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,782.4 ± 13.5 | 2,788.4 ± 21.1 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 350.5 ± 1.1 | 353.4 ± 0.7 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.4 ± 0.1 | 44.6 ± 0.1 | 216.0 | 216.0 |

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
| `Get_3` | `-` | 17.0 ± 0.1 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.0 ± 0.1 | 25.2 ± 0.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.9 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 405,980.5 ± 3,670.3 | — | 1,073,029.6 | — |
| `cModifyId` | `size=64` | 52,651.4 ± 738.3 | — | 136,396.4 | — |
| `cModifyId` | `size=8` | 8,897.7 ± 33.1 | — | 20,824.1 | — |
| `cReadId` | `size=512` | 217,011.6 ± 1,989.3 | — | 804,181.7 | — |
| `cReadId` | `size=64` | 28,334.1 ± 1,056.9 | — | 101,382.1 | — |
| `cReadId` | `size=8` | 4,355.0 ± 35.5 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 216,483.3 ± 1,978.7 | — | 804,349.5 | — |
| `cReadStreet` | `size=64` | 27,278.4 ± 146.6 | — | 101,548.9 | — |
| `cReadStreet` | `size=8` | 4,461.3 ± 37.1 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 411,015.4 ± 7,060.4 | — | 1,072,945.3 | — |
| `cReplaceId` | `size=64` | 52,763.7 ± 295.1 | — | 136,325.7 | — |
| `cReplaceId` | `size=8` | 8,841.9 ± 43.6 | — | 20,744.1 | — |
| `cSumPrices` | `size=512` | 350,790.7 ± 1,388.4 | — | 1,253,091.7 | — |
| `cSumPrices` | `size=64` | 43,452.0 ± 494.8 | — | 157,828.4 | — |
| `cSumPrices` | `size=8` | 6,541.6 ± 109.6 | — | 22,970.7 | — |
| `jMiss` | `size=512` | 166.9 ± 3.8 | — | 0.0 | — |
| `jMiss` | `size=64` | 171.4 ± 7.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 164.5 ± 1.5 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,406.4 ± 29.2 | — | 41,921.0 | — |
| `jModifyId` | `size=64` | 431.3 ± 3.0 | — | 5,344.0 | — |
| `jModifyId` | `size=8` | 107.5 ± 2.0 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.3 ± 2.3 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.5 ± 1.2 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.4 ± 1.2 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 187.3 ± 13.7 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 179.9 ± 1.1 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 178.9 ± 0.8 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,349.3 ± 24.6 | — | 41,889.0 | — |
| `jReplaceId` | `size=64` | 422.2 ± 2.5 | — | 5,312.0 | — |
| `jReplaceId` | `size=8` | 102.2 ± 2.7 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 84,840.4 ± 1,934.4 | — | 63,665.9 | — |
| `jSumPrices` | `size=64` | 10,538.3 ± 293.2 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,439.8 ± 21.9 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.1 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.7 ± 0.2 | 31.4 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.2 ± 0.0 | 3.1 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 54,017.8 ± 854.6 | — | 380,288.3 | — |
| `Modify_multiFocus` | `size=256` | 12,883.9 ± 53.6 | — | 88,400.9 | — |
| `Modify_multiFocus` | `size=32` | 1,553.5 ± 6.1 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 248.7 ± 0.9 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,792.6 ± 210.4 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,770.0 ± 78.4 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,109.4 ± 7.9 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 199.6 ± 16.1 | — | 936.0 | — |
| `naive_listMap` | `size=1024` | 8,311.7 ± 25.7 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,054.2 ± 9.2 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 247.8 ± 1.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.1 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.9 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 165.1 ± 0.9 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.8 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.3 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.3 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.4 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.7 ± 1.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.0 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,262.9 ± 43.5 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,158.3 ± 8.7 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.9 ± 0.0 | 23.1 ± 1.2 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.5 ± 0.2 | 71.2 ± 0.3 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.7 ± 1.8 | 115.9 ± 0.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.7 ± 0.0 | 20.7 ± 0.0 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.0 ± 0.0 | 6.8 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,329.6 ± 133.6 | — | 97,656.7 | — |
| `ModifyNames` | `size=64` | 4,725.3 ± 56.8 | — | 12,800.2 | — |
| `ModifyNames` | `size=8` | 668.5 ± 10.7 | — | 2,208.0 | — |
| `ModifyStreet` | `size=512` | 133.8 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 133.5 ± 1.3 | — | 320.0 | — |
| `ModifyStreet` | `size=8` | 133.9 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 40.8 ± 0.0 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.7 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.8 ± 0.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 103,770.0 ± 1,068.9 | — | 382,771.8 | — |
| `monocleModifyNames` | `size=64` | 10,457.2 ± 108.3 | — | 39,856.4 | — |
| `monocleModifyNames` | `size=8` | 1,532.2 ± 14.6 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,195.9 ± 181.2 | — | 169,084.5 | — |
| `monocleModifyStreet` | `size=64` | 7,289.0 ± 46.8 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 1,042.1 ± 10.5 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,842.1 ± 145.6 | — | 69,789.5 | — |
| `monocleReadStreet` | `size=64` | 4,234.1 ± 70.5 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 515.3 ± 9.2 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 70,039.6 ± 680.1 | — | 226,301.7 | — |
| `naiveModifyNames` | `size=64` | 8,988.0 ± 87.5 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,237.8 ± 22.9 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,452.6 ± 310.4 | — | 169,061.9 | — |
| `naiveModifyStreet` | `size=64` | 6,927.2 ± 405.0 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,027.7 ± 18.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,908.4 ± 88.6 | — | 69,789.4 | — |
| `naiveReadStreet` | `size=64` | 4,202.7 ± 26.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 514.4 ± 8.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 271,571.4 ± 4,926.3 | — | 642,765.8 | — |
| `Names` | `size=64` | 33,930.4 ± 198.3 | — | 82,955.5 | — |
| `Names` | `size=8` | 4,750.1 ± 27.0 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 305,850.4 ± 5,859.4 | — | 724,616.9 | — |
| `NamesIor` | `size=64` | 38,089.3 ± 885.3 | — | 90,130.1 | — |
| `NamesIor` | `size=8` | 4,964.0 ± 72.9 | — | 12,032.1 | — |
| `Street` | `size=512` | 1,174.9 ± 13.7 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,181.6 ± 15.4 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,166.6 ± 19.2 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,157.2 ± 3.8 | — | 2,984.9 | — |
| `StreetIor` | `size=64` | 1,186.4 ± 14.7 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,189.3 ± 5.8 | — | 2,984.0 | — |
| `directNames` | `size=512` | 259,407.6 ± 5,195.3 | — | 609,900.3 | — |
| `directNames` | `size=64` | 32,326.4 ± 409.0 | — | 77,712.5 | — |
| `directNames` | `size=8` | 4,472.2 ± 77.4 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 1,107.5 ± 16.4 | — | 2,744.9 | — |
| `directStreet` | `size=64` | 1,099.5 ± 11.8 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,091.2 ± 15.7 | — | 2,744.0 | — |
| `hcursorNames` | `size=512` | 260,146.5 ± 9,741.7 | — | 609,900.8 | — |
| `hcursorNames` | `size=64` | 32,611.4 ± 365.8 | — | 77,792.0 | — |
| `hcursorNames` | `size=8` | 4,537.6 ± 52.5 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,206.4 ± 20.4 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,204.5 ± 27.0 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,194.1 ± 23.4 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 238,293.1 ± 1,485.2 | — | 1,121,773.4 | — |
| `monocleNames` | `size=64` | 26,005.6 ± 334.0 | — | 132,759.4 | — |
| `monocleNames` | `size=8` | 4,027.4 ± 54.9 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 193,564.8 ± 2,021.5 | — | 908,039.4 | — |
| `monocleStreet` | `size=64` | 22,861.9 ± 100.5 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,450.7 ± 22.2 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 203,434.7 ± 2,375.6 | — | 965,272.7 | — |
| `naiveNames` | `size=64` | 24,369.5 ± 215.3 | — | 120,827.0 | — |
| `naiveNames` | `size=8` | 3,677.9 ± 24.3 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 191,245.6 ± 3,493.1 | — | 908,032.5 | — |
| `naiveStreet` | `size=64` | 22,816.5 ± 222.3 | — | 113,775.4 | — |
| `naiveStreet` | `size=8` | 3,463.8 ± 8.6 | — | 17,056.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,744.0 ± 18.5 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 603.2 ± 1.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 288.0 ± 9.1 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 182.0 ± 5.5 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 179.7 ± 1.3 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 181.8 ± 2.3 | — | 128.0 | — |
| `SumPrices` | `size=512` | 81,667.0 ± 1,271.1 | — | 63,718.6 | — |
| `SumPrices` | `size=64` | 10,264.7 ± 41.1 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,479.6 ± 29.0 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 168,639.8 ± 3,350.9 | — | 333,605.6 | — |
| `monocleModifyStreet` | `size=64` | 20,468.7 ± 70.0 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,396.7 ± 6.0 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,263.8 ± 303.3 | — | 193,265.6 | — |
| `monocleReadStreet` | `size=64` | 12,376.6 ± 206.3 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,932.9 ± 6.8 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 447,493.1 ± 1,518.9 | — | 1,190,799.8 | — |
| `monocleSumPrices` | `size=64` | 16,706.0 ± 51.1 | — | 47,425.7 | — |
| `monocleSumPrices` | `size=8` | 2,632.0 ± 3.5 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 166,712.4 ± 789.3 | — | 333,602.6 | — |
| `naiveModifyStreet` | `size=64` | 20,641.0 ± 171.9 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,428.5 ± 59.5 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 95,554.9 ± 739.1 | — | 193,265.8 | — |
| `naiveReadStreet` | `size=64` | 12,508.8 ± 587.3 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,929.4 ± 15.4 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 101,195.6 ± 2,248.3 | — | 230,158.6 | — |
| `naiveSumPrices` | `size=64` | 12,744.5 ± 38.3 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,011.9 ± 7.0 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 38,498.4 ± 1,635.8 | — | 455.7 | — |
| `nativeReadStreet` | `size=64` | 4,749.1 ± 11.5 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 803.8 ± 1.9 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,778.0 ± 777.7 | — | 86,269.7 | — |
| `nativeSumPrices` | `size=64` | 8,058.4 ± 94.4 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,190.1 ± 10.3 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 132,428.9 ± 582.8 | — | 624,392.4 | — |
| `TransformDeep` | `n=512` | 13,036.4 ± 63.4 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,620.8 ± 21.4 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 146,382.7 ± 2,327.5 | 177,384.6 ± 1,723.0 | 655,370.5 | 753,745.1 |
| `TransformExpr` | `n=512` | 18,253.2 ± 94.9 | 16,095.3 ± 178.2 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,245.1 ± 15.0 | 2,738.9 ± 27.5 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 102,110.5 ± 536.7 | — | 786,578.3 | — |
| `UniverseDeep` | `n=512` | 15,375.2 ± 32.9 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,871.4 ± 4.3 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 98,413.1 ± 576.5 | 1,745,859.5 ± 59,500.0 | 786,383.6 | 4,752,485.6 |
| `UniverseExpr` | `n=512` | 14,657.9 ± 261.2 | 176,246.4 ± 7,642.2 | 98,185.5 | 483,202.2 |
| `UniverseExpr` | `n=64` | 1,758.7 ± 9.5 | 16,990.8 ± 496.3 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 182,269.4 ± 983.8 | 2,080,298.6 ± 71,721.5 | 786,444.6 | 6,489,056.6 |
| `UniverseJson` | `n=512` | 20,387.0 ± 174.4 | 214,485.9 ± 2,731.3 | 98,186.1 | 699,917.9 |
| `UniverseJson` | `n=64` | 2,471.6 ± 5.5 | 21,378.7 ± 172.9 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 35,129.4 ± 174.2 | — | 163,881.6 | — |
| `visitorTransformDeep` | `n=512` | 4,121.1 ± 27.7 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 434.9 ± 2.0 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 66,856.8 ± 130.4 | — | 360,472.7 | — |
| `visitorTransformExpr` | `n=512` | 8,388.4 ± 52.5 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,043.0 ± 3.2 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,862.3 ± 1,021.8 | — | 196,705.4 | — |
| `visitorUniverseDeep` | `n=512` | 6,781.9 ± 82.2 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 805.6 ± 5.4 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,349.3 ± 525.3 | — | 196,656.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,844.1 ± 36.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 837.9 ± 3.5 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 139,703.4 ± 4,105.5 | — | 294,997.7 | — |
| `visitorUniverseJson` | `n=512` | 13,946.9 ± 39.3 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,300.4 ± 256.3 | — | 5,264.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,907.8 ± 75.1 | — | 41,438.6 | — |
| `Modify_powerEach` | `size=16` | 294.8 ± 3.2 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,323.2 ± 40.3 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 143.8 ± 2.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 55,833.9 ± 678.2 | — | 164,425.5 | — |
| `Modify_powerEach` | `size=64` | 896.9 ± 7.2 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 63,064.7 ± 6,245.8 | — | 279,436.5 | — |
| `monocle_powerEach` | `size=16` | 646.5 ± 7.5 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 22,107.8 ± 184.0 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 260.4 ± 2.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 186,488.7 ± 5,069.5 | — | 967,873.9 | — |
| `monocle_powerEach` | `size=64` | 2,090.8 ± 34.7 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,533.9 ± 11.4 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.5 ± 0.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,611.8 ± 3.0 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.3 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,457.1 ± 138.2 | — | 114,782.8 | — |
| `naive_powerEach` | `size=64` | 397.2 ± 5.2 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 63,774.1 ± 266.0 | — | 210,924.7 | — |
| `Modify_nested` | `size=16` | 1,679.7 ± 13.0 | — | 5,269.4 | — |
| `Modify_nested` | `size=256` | 15,977.5 ± 165.0 | — | 54,127.3 | — |
| `Modify_nested` | `size=4` | 830.7 ± 12.5 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,094.2 ± 224.1 | — | 15,120.5 | — |
| `monocle_nested` | `size=1024` | 256,698.3 ± 793.7 | — | 1,118,892.6 | — |
| `monocle_nested` | `size=16` | 2,915.0 ± 24.3 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 95,965.7 ± 3,293.0 | — | 430,213.6 | — |
| `monocle_nested` | `size=4` | 1,405.2 ± 227.2 | — | 5,546.7 | — |
| `monocle_nested` | `size=64` | 8,877.9 ± 124.7 | — | 58,907.7 | — |
| `naive_nested` | `size=1024` | 21,624.4 ± 957.0 | — | 115,073.5 | — |
| `naive_nested` | `size=16` | 390.7 ± 6.5 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,818.3 ± 53.0 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 135.2 ± 3.9 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,412.6 ± 14.7 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,555.0 ± 29.0 | — | 4,933.4 | — |
| `Modify_sparse` | `size=2048` | 28,891.5 ± 287.8 | — | 104,801.4 | — |
| `Modify_sparse` | `size=32` | 517.1 ± 2.9 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,153.7 ± 26.1 | — | 24,903.3 | — |
| `Modify_sparse` | `size=8` | 176.7 ± 2.7 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,734.6 ± 31.2 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 100,039.0 ± 791.4 | — | 476,033.1 | — |
| `monocle_sparse` | `size=32` | 1,009.1 ± 25.0 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,981.6 ± 222.0 | — | 156,443.1 | — |
| `monocle_sparse` | `size=8` | 314.8 ± 3.9 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 326.6 ± 0.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,430.9 ± 22.9 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,290.2 ± 2.6 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.5 ± 0.7 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.4 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.6 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 142,791.5 ± 632.7 | — | 786,297.0 | — |
| `Cata` | `-` | 84,623.5 ± 184.6 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,400.9 ± 864.7 | — | 295,848.6 | — |
| `drosteAna` | `-` | 57,203.6 ± 582.9 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,668.1 ± 280.6 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,568.4 ± 546.3 | — | 328,640.5 | — |
| `handAna` | `-` | 20,318.0 ± 1,520.4 | — | 163,816.1 | — |
| `handCata` | `-` | 13,128.9 ± 46.4 | — | 0.1 | — |
| `handHylo` | `-` | 11,433.9 ± 207.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.6 ± 0.0 | 26.0 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.0 ± 0.1 | 60.1 ± 1.0 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.0 ± 0.0 | 3.0 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,047.0 ± 72.5 | 34,824.1 ± 1,095.8 | 39,001.1 | 176,912.7 |
| `Modify` | `size=64` | 960.6 ± 2.2 | 1,766.1 ± 10.5 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 110.3 ± 1.1 | 289.9 ± 4.4 | 728.0 | 1,936.0 |

