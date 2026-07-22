# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `b4b1f2c271aa9094151d6d5ca75063386623f18e` · date: `2026-07-22` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.1 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 32.6 ± 1.9 | 23.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 164.9 ± 2.7 | — | 720.0 | — |
| `ModifyCountry` | `-` | 362.1 ± 10.0 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 448.0 ± 8.6 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 174.4 ± 3.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 208.5 ± 5.7 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 317.8 ± 3.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,676.7 ± 14.6 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,706.5 ± 63.2 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,108.1 ± 112.2 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,717.6 ± 16.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,722.9 ± 7.5 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 769.3 ± 9.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 588.6 ± 9.9 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 404.5 ± 2.1 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 428.9 ± 2.3 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,329.3 ± 9.3 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,293.6 ± 9.2 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,487.9 ± 30.0 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,982.2 ± 31.0 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 1,010.3 ± 14.8 | — | 6,552.0 | — |
| `WideToJson` | `-` | 701.6 ± 28.7 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,774.1 ± 26.6 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,788.2 ± 7.1 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,023.8 ± 15.3 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,978.1 ± 64.9 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 234.6 ± 8.5 | — | 984.0 | — |
| `decode_native` | `-` | 18.6 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 221.4 ± 0.9 | — | 984.0 | — |
| `encode_bridged` | `-` | 261.6 ± 7.2 | — | 1,277.3 | — |
| `encode_native` | `-` | 14.7 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 256.0 ± 10.5 | — | 1,282.7 | — |
| `fieldGet_bridged` | `-` | 96.9 ± 0.4 | — | 432.0 | — |
| `fieldGet_native` | `-` | 97.6 ± 2.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 446.7 ± 7.2 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 175.2 ± 0.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.3 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.9 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.5 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.4 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.2 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.4 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 41.4 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.4 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.2 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 133.5 ± 0.8 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.6 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,430.2 ± 31.6 | 4,504.2 ± 18.3 | 14,080.7 | 14,080.7 |
| `FoldMap` | `size=64` | 326.0 ± 1.8 | 307.3 ± 0.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.9 ± 3.2 | 20.7 ± 0.5 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,780.3 ± 5.1 | 2,778.3 ± 9.5 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 349.4 ± 0.7 | 354.5 ± 3.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.2 ± 0.1 | 44.6 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
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
| `Get_3` | `-` | 16.9 ± 0.0 | 8.1 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.1 ± 0.2 | 26.2 ± 0.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.9 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 406,977.4 ± 3,271.0 | — | 1,073,010.3 | — |
| `cModifyId` | `size=64` | 51,938.0 ± 162.4 | — | 136,431.5 | — |
| `cModifyId` | `size=8` | 8,894.2 ± 26.7 | — | 20,824.1 | — |
| `cReadId` | `size=512` | 215,786.8 ± 1,624.4 | — | 804,181.3 | — |
| `cReadId` | `size=64` | 27,530.9 ± 483.7 | — | 101,396.9 | — |
| `cReadId` | `size=8` | 4,431.2 ± 54.8 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 220,129.5 ± 5,708.8 | — | 804,350.6 | — |
| `cReadStreet` | `size=64` | 27,288.3 ± 327.0 | — | 101,564.7 | — |
| `cReadStreet` | `size=8` | 4,480.8 ± 55.2 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 406,424.7 ± 3,000.9 | — | 1,072,943.0 | — |
| `cReplaceId` | `size=64` | 52,295.8 ± 358.3 | — | 136,304.1 | — |
| `cReplaceId` | `size=8` | 8,857.1 ± 54.6 | — | 20,760.1 | — |
| `cSumPrices` | `size=512` | 348,269.9 ± 2,707.3 | — | 1,253,091.0 | — |
| `cSumPrices` | `size=64` | 43,731.4 ± 781.1 | — | 157,808.3 | — |
| `cSumPrices` | `size=8` | 6,739.5 ± 177.8 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 181.5 ± 0.8 | — | 0.1 | — |
| `jMiss` | `size=64` | 183.7 ± 3.1 | — | 0.0 | — |
| `jMiss` | `size=8` | 171.8 ± 0.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,559.0 ± 137.5 | — | 41,929.0 | — |
| `jModifyId` | `size=64` | 440.5 ± 5.2 | — | 5,344.0 | — |
| `jModifyId` | `size=8` | 105.9 ± 0.3 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.5 ± 1.5 | — | 56.0 | — |
| `jReadId` | `size=64` | 36.1 ± 1.3 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.0 ± 0.1 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 202.4 ± 8.0 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 204.8 ± 9.4 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 197.3 ± 0.7 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,412.3 ± 26.3 | — | 41,889.0 | — |
| `jReplaceId` | `size=64` | 426.0 ± 3.9 | — | 5,312.0 | — |
| `jReplaceId` | `size=8` | 100.0 ± 1.6 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 87,490.1 ± 1,449.7 | — | 63,664.3 | — |
| `jSumPrices` | `size=64` | 10,528.4 ± 169.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,469.1 ± 22.6 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.9 ± 0.1 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.6 ± 0.3 | 31.4 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,112.5 ± 706.2 | — | 380,249.5 | — |
| `Modify_multiFocus` | `size=256` | 12,825.0 ± 52.4 | — | 88,400.9 | — |
| `Modify_multiFocus` | `size=32` | 1,558.7 ± 16.3 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 248.0 ± 4.3 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,843.8 ± 150.7 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,753.3 ± 85.1 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,014.3 ± 138.9 | — | 3,272.0 | — |
| `Modify_powerEach` | `size=4` | 200.0 ± 15.0 | — | 936.0 | — |
| `naive_listMap` | `size=1024` | 8,311.4 ± 27.4 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,056.2 ± 9.4 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 247.2 ± 1.0 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.0 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 67.1 ± 0.3 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 165.6 ± 1.5 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.4 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.8 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 38.7 ± 2.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.7 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.1 ± 0.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.9 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,244.9 ± 17.5 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,152.2 ± 22.5 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.9 ± 0.1 | 22.3 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.6 ± 0.1 | 70.9 ± 0.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 152.2 ± 0.8 | 116.2 ± 1.0 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.9 ± 0.3 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.2 ± 0.1 | 7.0 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,224.7 ± 127.6 | — | 97,440.8 | — |
| `ModifyNames` | `size=64` | 4,791.8 ± 101.2 | — | 13,592.2 | — |
| `ModifyNames` | `size=8` | 650.8 ± 4.4 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 134.1 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 134.9 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 134.1 ± 0.5 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 39.4 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 39.3 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.5 ± 2.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 103,084.5 ± 720.4 | — | 382,772.5 | — |
| `monocleModifyNames` | `size=64` | 9,969.2 ± 611.6 | — | 39,832.3 | — |
| `monocleModifyNames` | `size=8` | 1,572.7 ± 60.9 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 59,158.8 ± 5,588.6 | — | 169,085.7 | — |
| `monocleModifyStreet` | `size=64` | 7,229.4 ± 37.4 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 1,029.9 ± 5.5 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,603.8 ± 194.7 | — | 69,789.4 | — |
| `monocleReadStreet` | `size=64` | 4,195.0 ± 21.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 519.9 ± 3.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,172.5 ± 337.6 | — | 226,301.3 | — |
| `naiveModifyNames` | `size=64` | 8,922.4 ± 52.7 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,220.1 ± 6.5 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,323.0 ± 355.3 | — | 169,061.9 | — |
| `naiveModifyStreet` | `size=64` | 6,904.1 ± 385.5 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,019.8 ± 7.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 33,482.3 ± 914.4 | — | 69,789.9 | — |
| `naiveReadStreet` | `size=64` | 4,186.6 ± 31.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 507.6 ± 3.2 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 270,817.7 ± 2,339.9 | — | 646,869.2 | — |
| `Names` | `size=64` | 33,622.5 ± 208.3 | — | 82,947.4 | — |
| `Names` | `size=8` | 4,801.6 ± 72.4 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 302,440.1 ± 4,874.0 | — | 724,614.2 | — |
| `NamesIor` | `size=64` | 37,350.8 ± 408.1 | — | 89,610.4 | — |
| `NamesIor` | `size=8` | 4,894.8 ± 79.9 | — | 11,928.1 | — |
| `Street` | `size=512` | 1,155.4 ± 34.3 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,174.8 ± 11.8 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,149.9 ± 26.5 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,161.1 ± 11.4 | — | 2,984.9 | — |
| `StreetIor` | `size=64` | 1,165.7 ± 10.2 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,145.7 ± 14.5 | — | 2,984.0 | — |
| `directNames` | `size=512` | 260,406.1 ± 12,177.0 | — | 614,021.1 | — |
| `directNames` | `size=64` | 32,836.1 ± 325.7 | — | 77,718.1 | — |
| `directNames` | `size=8` | 4,492.2 ± 63.2 | — | 10,698.8 | — |
| `directStreet` | `size=512` | 1,111.4 ± 19.2 | — | 2,752.9 | — |
| `directStreet` | `size=64` | 1,089.9 ± 7.9 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,102.7 ± 28.2 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 258,393.5 ± 3,184.2 | — | 614,003.5 | — |
| `hcursorNames` | `size=64` | 31,819.0 ± 642.1 | — | 77,790.6 | — |
| `hcursorNames` | `size=8` | 4,552.2 ± 30.4 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,187.8 ± 11.3 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,201.5 ± 24.3 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,191.2 ± 17.6 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 238,282.3 ± 2,689.0 | — | 1,121,768.1 | — |
| `monocleNames` | `size=64` | 26,221.7 ± 465.7 | — | 132,749.1 | — |
| `monocleNames` | `size=8` | 4,009.2 ± 51.5 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 194,008.9 ± 4,634.7 | — | 908,039.7 | — |
| `monocleStreet` | `size=64` | 22,685.5 ± 160.5 | — | 113,804.7 | — |
| `monocleStreet` | `size=8` | 3,479.3 ± 23.0 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 203,494.6 ± 1,681.7 | — | 965,272.7 | — |
| `naiveNames` | `size=64` | 24,424.7 ± 162.0 | — | 120,826.3 | — |
| `naiveNames` | `size=8` | 3,667.8 ± 19.8 | — | 17,818.7 | — |
| `naiveStreet` | `size=512` | 187,795.2 ± 1,138.4 | — | 908,040.8 | — |
| `naiveStreet` | `size=64` | 22,623.2 ± 70.6 | — | 113,780.7 | — |
| `naiveStreet` | `size=8` | 3,462.0 ± 15.3 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,728.5 ± 29.7 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 612.6 ± 17.2 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 333.5 ± 12.0 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 197.9 ± 1.1 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 199.6 ± 2.2 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 203.3 ± 4.7 | — | 114.7 | — |
| `SumPrices` | `size=512` | 87,230.6 ± 3,395.6 | — | 63,720.1 | — |
| `SumPrices` | `size=64` | 10,705.7 ± 465.1 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,463.0 ± 5.3 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 165,735.0 ± 557.4 | — | 333,576.5 | — |
| `monocleModifyStreet` | `size=64` | 20,636.9 ± 118.1 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,417.6 ± 34.2 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,933.8 ± 1,108.2 | — | 193,266.1 | — |
| `monocleReadStreet` | `size=64` | 12,292.1 ± 186.2 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,951.1 ± 83.8 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 448,443.6 ± 3,063.9 | — | 1,190,800.8 | — |
| `monocleSumPrices` | `size=64` | 16,712.1 ± 69.1 | — | 47,420.4 | — |
| `monocleSumPrices` | `size=8` | 2,683.8 ± 97.2 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 168,445.6 ± 1,728.6 | — | 333,570.1 | — |
| `naiveModifyStreet` | `size=64` | 20,549.3 ± 53.0 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,413.3 ± 16.6 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 95,503.1 ± 1,042.6 | — | 193,265.7 | — |
| `naiveReadStreet` | `size=64` | 12,112.4 ± 124.7 | — | 24,737.2 | — |
| `naiveReadStreet` | `size=8` | 1,931.2 ± 8.8 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 102,249.6 ± 4,029.4 | — | 230,159.5 | — |
| `naiveSumPrices` | `size=64` | 12,764.7 ± 78.0 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,022.5 ± 7.8 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 37,691.1 ± 574.2 | — | 455.0 | — |
| `nativeReadStreet` | `size=64` | 4,764.9 ± 17.1 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 803.5 ± 2.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 64,699.6 ± 646.0 | — | 86,268.3 | — |
| `nativeSumPrices` | `size=64` | 8,039.8 ± 33.4 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,201.5 ± 12.4 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 132,457.2 ± 2,272.0 | — | 624,392.4 | — |
| `TransformDeep` | `n=512` | 12,983.3 ± 40.9 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,607.5 ± 9.2 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 145,365.0 ± 1,971.5 | 176,494.6 ± 1,329.5 | 655,369.8 | 753,744.4 |
| `TransformExpr` | `n=512` | 18,185.3 ± 74.0 | 15,985.3 ± 119.8 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,262.2 ± 19.1 | 2,724.5 ± 7.4 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 98,713.4 ± 938.1 | — | 786,575.8 | — |
| `UniverseDeep` | `n=512` | 15,366.2 ± 50.4 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,868.3 ± 11.0 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 97,689.1 ± 1,157.3 | 1,743,164.9 ± 64,856.1 | 786,383.1 | 4,752,483.6 |
| `UniverseExpr` | `n=512` | 14,507.7 ± 96.2 | 170,900.5 ± 1,901.9 | 98,185.5 | 483,201.4 |
| `UniverseExpr` | `n=64` | 1,755.3 ± 12.4 | 16,725.0 ± 653.5 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 182,111.0 ± 943.1 | 1,944,236.4 ± 6,890.8 | 786,444.5 | 6,488,957.9 |
| `UniverseJson` | `n=512` | 20,378.9 ± 126.5 | 214,868.4 ± 3,673.9 | 98,186.1 | 699,917.9 |
| `UniverseJson` | `n=64` | 2,463.6 ± 16.0 | 21,158.9 ± 368.2 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,825.3 ± 252.9 | — | 163,881.3 | — |
| `visitorTransformDeep` | `n=512` | 4,120.2 ± 48.0 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 439.0 ± 10.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,190.1 ± 326.5 | — | 360,472.9 | — |
| `visitorTransformExpr` | `n=512` | 8,311.2 ± 33.0 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,036.3 ± 2.9 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,951.8 ± 785.6 | — | 196,705.5 | — |
| `visitorUniverseDeep` | `n=512` | 6,890.2 ± 87.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 804.2 ± 3.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,278.4 ± 569.2 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,816.3 ± 12.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 834.3 ± 1.5 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 155,008.2 ± 23,312.5 | — | 313,728.8 | — |
| `visitorUniverseJson` | `n=512` | 14,446.7 ± 1,028.3 | — | 36,851.2 | — |
| `visitorUniverseJson` | `n=64` | 2,165.7 ± 300.4 | — | 4,880.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,949.3 ± 89.7 | — | 41,438.6 | — |
| `Modify_powerEach` | `size=16` | 299.0 ± 5.7 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,340.5 ± 21.5 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 142.1 ± 7.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 56,938.7 ± 1,150.5 | — | 164,428.3 | — |
| `Modify_powerEach` | `size=64` | 916.7 ± 18.2 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 59,645.5 ± 727.0 | — | 279,434.0 | — |
| `monocle_powerEach` | `size=16` | 632.5 ± 10.3 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,692.5 ± 137.8 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 243.9 ± 19.2 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 183,726.9 ± 3,084.2 | — | 967,869.2 | — |
| `monocle_powerEach` | `size=64` | 2,076.5 ± 9.8 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,551.9 ± 65.5 | — | 28,730.8 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,611.7 ± 2.3 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.0 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,379.7 ± 92.4 | — | 114,782.7 | — |
| `naive_powerEach` | `size=64` | 394.4 ± 0.7 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 66,177.0 ± 3,048.3 | — | 210,978.1 | — |
| `Modify_nested` | `size=16` | 1,691.4 ± 23.7 | — | 5,208.1 | — |
| `Modify_nested` | `size=256` | 16,451.8 ± 731.9 | — | 54,143.6 | — |
| `Modify_nested` | `size=4` | 814.0 ± 32.3 | — | 2,680.0 | — |
| `Modify_nested` | `size=64` | 4,312.6 ± 309.9 | — | 15,112.5 | — |
| `monocle_nested` | `size=1024` | 254,627.1 ± 2,230.3 | — | 1,118,888.6 | — |
| `monocle_nested` | `size=16` | 2,879.4 ± 88.1 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 93,704.9 ± 1,261.4 | — | 430,212.8 | — |
| `monocle_nested` | `size=4` | 1,296.7 ± 38.6 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,964.2 ± 139.1 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 21,560.4 ± 953.4 | — | 115,073.4 | — |
| `naive_nested` | `size=16` | 390.4 ± 7.4 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,819.8 ± 48.0 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 134.3 ± 3.3 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,412.6 ± 12.2 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,535.2 ± 5.6 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 28,714.4 ± 308.5 | — | 104,801.1 | — |
| `Modify_sparse` | `size=32` | 522.1 ± 11.1 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,113.9 ± 21.7 | — | 24,906.0 | — |
| `Modify_sparse` | `size=8` | 177.2 ± 2.9 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,687.7 ± 20.0 | — | 24,722.8 | — |
| `monocle_sparse` | `size=2048` | 99,239.2 ± 723.6 | — | 476,033.1 | — |
| `monocle_sparse` | `size=32` | 1,001.1 ± 11.7 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 32,136.5 ± 514.9 | — | 156,443.2 | — |
| `monocle_sparse` | `size=8` | 306.5 ± 5.9 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 326.5 ± 0.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,426.8 ± 27.6 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 84.0 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,292.0 ± 7.1 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.6 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 143,277.2 ± 1,848.9 | — | 786,297.0 | — |
| `Cata` | `-` | 99,701.8 ± 24,746.8 | — | 197,568.7 | — |
| `Hylo` | `-` | 89,689.9 ± 2,488.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 56,894.9 ± 313.1 | — | 327,632.4 | — |
| `drosteCata` | `-` | 45,366.7 ± 1,341.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,163.6 ± 582.8 | — | 328,640.5 | — |
| `handAna` | `-` | 19,719.2 ± 497.3 | — | 163,816.1 | — |
| `handCata` | `-` | 13,124.8 ± 20.5 | — | 0.1 | — |
| `handHylo` | `-` | 11,006.8 ± 292.1 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.6 ± 0.1 | 26.1 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.1 ± 0.2 | 59.4 ± 0.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.1 ± 0.0 | 3.1 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 7,949.5 ± 49.6 | 35,713.9 ± 259.1 | 38,961.0 | 176,924.5 |
| `Modify` | `size=64` | 939.7 ± 5.1 | 1,765.5 ± 9.3 | 4,864.0 | 14,448.0 |
| `Modify` | `size=8` | 113.6 ± 0.1 | 288.3 ± 3.9 | 688.0 | 1,936.0 |

