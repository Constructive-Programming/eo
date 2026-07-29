# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `e5bb891eb6ba57b284b837a2886bcaf3317a96af` · date: `2026-07-28` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 11.1 ± 0.1 | 8.0 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 23.6 ± 0.1 | 20.5 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 126.1 ± 1.2 | — | 720.0 | — |
| `ModifyCountry` | `-` | 582.5 ± 4.6 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 637.3 ± 8.3 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 146.0 ± 2.0 | — | 520.0 | — |
| `ReadPartner` | `-` | 172.2 ± 1.8 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 254.7 ± 1.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,640.2 ± 12.6 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,652.8 ± 22.8 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,068.4 ± 52.1 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,486.5 ± 29.1 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,588.8 ± 63.2 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 786.2 ± 14.7 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 580.9 ± 16.1 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 318.3 ± 1.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 356.9 ± 7.1 | — | 1,541.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,280.1 ± 8.2 | — | 3,677.3 | — |
| `freshDecodeRecord` | `-` | 1,210.5 ± 18.6 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,628.0 ± 23.5 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,616.4 ± 18.2 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 1,373.2 ± 14.3 | — | 6,552.0 | — |
| `WideToJson` | `-` | 559.5 ± 9.5 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,660.0 ± 13.9 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,500.3 ± 17.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,131.1 ± 26.7 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,747.6 ± 59.8 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 188.2 ± 1.5 | — | 984.0 | — |
| `decode_native` | `-` | 15.9 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 187.0 ± 4.8 | — | 984.0 | — |
| `encode_bridged` | `-` | 207.6 ± 1.5 | — | 1,272.0 | — |
| `encode_native` | `-` | 12.5 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 209.9 ± 3.0 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 88.7 ± 3.1 | — | 432.0 | — |
| `fieldGet_native` | `-` | 86.8 ± 0.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 359.6 ± 5.7 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 143.7 ± 1.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 22.4 ± 1.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 23.8 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 22.2 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.7 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.0 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 28.8 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.9 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.8 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 10.6 ± 0.2 | — | 72.0 | — |
| `buildLens3` | `-` | 29.1 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 55.8 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 28.8 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.3 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 35.3 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 103.1 ± 0.8 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 49.8 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,415.6 ± 19.8 | 3,399.2 ± 52.5 | 14,080.6 | 14,080.6 |
| `FoldMap` | `size=64` | 247.0 ± 1.0 | 260.5 ± 1.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.3 ± 0.1 | 23.1 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,026.2 ± 42.3 | 2,972.0 ± 21.7 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 307.2 ± 1.1 | 306.5 ± 2.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 41.2 ± 0.2 | 41.4 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.0 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.6 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.6 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.5 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.8 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 13.0 ± 0.0 | 6.7 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 25.4 ± 0.1 | 19.8 ± 0.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.8 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.8 ± 0.0 | 4.9 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.7 ± 0.0 | 4.7 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 325,880.5 ± 1,702.4 | — | 1,072,895.3 | — |
| `cModifyId` | `size=64` | 42,743.2 ± 363.4 | — | 136,323.2 | — |
| `cModifyId` | `size=8` | 7,163.3 ± 51.9 | — | 20,760.1 | — |
| `cReadId` | `size=512` | 175,043.7 ± 1,033.9 | — | 804,105.8 | — |
| `cReadId` | `size=64` | 22,606.9 ± 305.0 | — | 101,312.6 | — |
| `cReadId` | `size=8` | 3,505.5 ± 21.9 | — | 15,608.0 | — |
| `cReadStreet` | `size=512` | 178,592.9 ± 2,602.3 | — | 804,106.8 | — |
| `cReadStreet` | `size=64` | 22,701.8 ± 163.7 | — | 101,328.7 | — |
| `cReadStreet` | `size=8` | 3,587.2 ± 47.3 | — | 15,608.0 | — |
| `cReplaceId` | `size=512` | 331,130.1 ± 3,787.3 | — | 1,072,814.7 | — |
| `cReplaceId` | `size=64` | 42,777.3 ± 419.4 | — | 136,243.3 | — |
| `cReplaceId` | `size=8` | 7,185.1 ± 30.9 | — | 21,184.1 | — |
| `cSumPrices` | `size=512` | 289,709.1 ± 2,549.4 | — | 1,250,930.4 | — |
| `cSumPrices` | `size=64` | 36,719.1 ± 230.4 | — | 157,764.2 | — |
| `cSumPrices` | `size=8` | 5,557.8 ± 60.9 | — | 22,754.7 | — |
| `jMiss` | `size=512` | 166.1 ± 1.2 | — | 0.0 | — |
| `jMiss` | `size=64` | 167.4 ± 0.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 166.2 ± 1.5 | — | 0.0 | — |
| `jModifyId` | `size=512` | 6,214.7 ± 41.5 | — | 41,937.8 | — |
| `jModifyId` | `size=64` | 809.8 ± 6.4 | — | 5,352.0 | — |
| `jModifyId` | `size=8` | 159.1 ± 1.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 34.0 ± 0.3 | — | 48.0 | — |
| `jReadId` | `size=64` | 33.5 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 33.3 ± 0.2 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 176.7 ± 4.0 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 173.6 ± 2.0 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 173.6 ± 0.4 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 6,034.2 ± 40.5 | — | 41,889.7 | — |
| `jReplaceId` | `size=64` | 803.3 ± 5.5 | — | 5,320.0 | — |
| `jReplaceId` | `size=8` | 155.5 ± 0.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 80,090.3 ± 735.6 | — | 63,670.2 | — |
| `jSumPrices` | `size=64` | 10,034.6 ± 69.4 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,360.5 ± 18.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 58.2 ± 0.3 | — | 312.0 | — |
| `MapDrillModify` | `-` | 40.1 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 41.1 ± 0.4 | — | 200.0 | — |
| `handEnvUse` | `-` | 56.9 ± 0.3 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 37.9 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 38.0 ± 0.2 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.8 ± 0.0 | 5.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 28.7 ± 0.1 | 29.7 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 5.7 ± 0.0 | 5.6 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 19,596.6 ± 2,957.5 | — | 43,037.1 | — |
| `Fold_powerEach` | `size=256` | 3,540.2 ± 17.0 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 412.1 ± 2.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 68.2 ± 0.4 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 72,067.7 ± 3,119.1 | — | 380,262.4 | — |
| `Modify_multiFocus` | `size=256` | 16,630.5 ± 775.3 | — | 88,403.8 | — |
| `Modify_multiFocus` | `size=32` | 1,893.8 ± 16.1 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 243.8 ± 4.0 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,839.8 ± 622.8 | — | 115,201.3 | — |
| `Modify_powerEach` | `size=256` | 7,514.5 ± 550.9 | — | 26,120.5 | — |
| `Modify_powerEach` | `size=32` | 868.9 ± 61.3 | — | 3,208.0 | — |
| `Modify_powerEach` | `size=4` | 146.7 ± 0.8 | — | 872.0 | — |
| `naive_listMap` | `size=1024` | 11,155.1 ± 104.4 | — | 65,578.9 | — |
| `naive_listMap` | `size=256` | 2,731.6 ± 9.9 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 344.3 ± 1.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 49.0 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 6,103.1 ± 100.5 | — | 16,129.6 | — |
| `naive_sumQty` | `size=256` | 726.6 ± 4.4 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 70.9 ± 1.0 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 6.8 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 49.5 ± 0.8 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 186.7 ± 3.7 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 19.8 ± 0.4 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 34.8 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 40.0 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 13.5 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 25.7 ± 0.5 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 222.1 ± 4.3 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 43.0 ± 0.6 | — | 184.0 | — |
| `buildAndUse` | `-` | 930.3 ± 15.5 | — | 2,816.0 | — |
| `reuseUse` | `-` | 873.1 ± 17.3 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.0 ± 0.1 | 20.7 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 49.6 ± 0.2 | 58.1 ± 0.6 | 160.0 | 304.0 |
| `Modify_6` | `-` | 118.5 ± 0.8 | 92.9 ± 2.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 18.6 ± 2.3 | 20.1 ± 0.1 | 104.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 5.9 ± 0.1 | 5.8 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 12.4 ± 0.1 | 12.4 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 34,478.4 ± 231.4 | — | 97,408.3 | — |
| `ModifyNames` | `size=64` | 4,212.5 ± 13.5 | — | 12,568.1 | — |
| `ModifyNames` | `size=8` | 585.7 ± 7.2 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 127.1 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 127.8 ± 1.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 127.7 ± 0.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 37.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 37.0 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 36.9 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 92,218.3 ± 678.7 | — | 382,778.1 | — |
| `monocleModifyNames` | `size=64` | 9,061.1 ± 63.3 | — | 39,834.2 | — |
| `monocleModifyNames` | `size=8` | 1,382.8 ± 47.5 | — | 5,432.0 | — |
| `monocleModifyStreet` | `size=512` | 48,588.6 ± 532.8 | — | 169,081.4 | — |
| `monocleModifyStreet` | `size=64` | 6,253.6 ± 57.2 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 869.7 ± 5.7 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 28,605.7 ± 249.4 | — | 69,785.6 | — |
| `monocleReadStreet` | `size=64` | 3,640.6 ± 31.3 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 433.7 ± 0.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 60,221.4 ± 191.2 | — | 226,296.5 | — |
| `naiveModifyNames` | `size=64` | 7,250.1 ± 430.7 | — | 27,920.2 | — |
| `naiveModifyNames` | `size=8` | 1,024.8 ± 16.0 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 48,555.3 ± 345.1 | — | 169,058.2 | — |
| `naiveModifyStreet` | `size=64` | 6,135.3 ± 486.4 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 895.8 ± 48.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 28,707.3 ± 475.6 | — | 69,785.4 | — |
| `naiveReadStreet` | `size=64` | 3,712.9 ± 25.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 441.8 ± 6.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 198,271.4 ± 3,126.3 | — | 609,893.0 | — |
| `Names` | `size=64` | 26,202.9 ± 727.1 | — | 78,778.8 | — |
| `Names` | `size=8` | 3,599.0 ± 124.5 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 211,823.5 ± 6,228.6 | — | 679,406.8 | — |
| `NamesIor` | `size=64` | 28,407.2 ± 374.9 | — | 87,531.5 | — |
| `NamesIor` | `size=8` | 3,796.4 ± 43.5 | — | 11,592.1 | — |
| `Street` | `size=512` | 911.0 ± 37.7 | — | 2,720.7 | — |
| `Street` | `size=64` | 903.8 ± 5.3 | — | 2,720.1 | — |
| `Street` | `size=8` | 903.5 ± 8.7 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 905.6 ± 11.2 | — | 2,736.7 | — |
| `StreetIor` | `size=64` | 909.4 ± 7.9 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 902.2 ± 13.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 188,947.2 ± 3,019.8 | — | 601,642.1 | — |
| `directNames` | `size=64` | 26,036.9 ± 704.5 | — | 77,710.3 | — |
| `directNames` | `size=8` | 3,574.2 ± 40.1 | — | 10,621.4 | — |
| `directStreet` | `size=512` | 906.7 ± 9.0 | — | 2,744.7 | — |
| `directStreet` | `size=64` | 901.7 ± 15.5 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 900.4 ± 13.3 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 195,468.5 ± 3,318.3 | — | 613,953.9 | — |
| `hcursorNames` | `size=64` | 25,338.7 ± 435.5 | — | 77,780.2 | — |
| `hcursorNames` | `size=8` | 3,510.5 ± 134.6 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 969.3 ± 14.8 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 971.5 ± 16.7 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 959.3 ± 15.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 202,947.1 ± 1,437.9 | — | 1,121,749.7 | — |
| `monocleNames` | `size=64` | 23,475.9 ± 130.0 | — | 132,757.2 | — |
| `monocleNames` | `size=8` | 3,783.9 ± 36.7 | — | 19,504.1 | — |
| `monocleStreet` | `size=512` | 156,662.5 ± 1,507.6 | — | 908,009.3 | — |
| `monocleStreet` | `size=64` | 20,039.4 ± 135.7 | — | 113,809.8 | — |
| `monocleStreet` | `size=8` | 3,192.4 ± 52.5 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 167,278.2 ± 826.1 | — | 965,269.7 | — |
| `naiveNames` | `size=64` | 21,702.9 ± 154.5 | — | 120,825.9 | — |
| `naiveNames` | `size=8` | 3,305.6 ± 20.8 | — | 17,818.7 | — |
| `naiveStreet` | `size=512` | 156,231.7 ± 1,299.1 | — | 908,014.3 | — |
| `naiveStreet` | `size=64` | 20,011.7 ± 127.7 | — | 113,780.5 | — |
| `naiveStreet` | `size=8` | 3,165.8 ± 48.0 | — | 17,034.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 6,234.0 ± 40.6 | — | 42,029.6 | — |
| `ModifyStreet` | `size=64` | 892.0 ± 14.0 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 270.0 ± 5.7 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 173.3 ± 1.4 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 173.4 ± 1.4 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 175.3 ± 0.9 | — | 128.0 | — |
| `SumPrices` | `size=512` | 79,937.0 ± 686.8 | — | 63,719.4 | — |
| `SumPrices` | `size=64` | 10,030.1 ± 207.9 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,343.4 ± 9.2 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 146,599.9 ± 412.3 | — | 333,559.8 | — |
| `monocleModifyStreet` | `size=64` | 17,831.6 ± 329.9 | — | 30,113.8 | — |
| `monocleModifyStreet` | `size=8` | 3,092.8 ± 29.4 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 77,184.3 ± 649.6 | — | 193,250.1 | — |
| `monocleReadStreet` | `size=64` | 9,774.4 ± 39.6 | — | 24,737.0 | — |
| `monocleReadStreet` | `size=8` | 1,562.0 ± 13.3 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 386,498.1 ± 6,465.3 | — | 1,190,746.8 | — |
| `monocleSumPrices` | `size=64` | 14,146.2 ± 65.8 | — | 47,417.4 | — |
| `monocleSumPrices` | `size=8` | 2,347.6 ± 101.2 | — | 6,680.0 | — |
| `naiveModifyStreet` | `size=512` | 146,519.7 ± 789.9 | — | 333,551.7 | — |
| `naiveModifyStreet` | `size=64` | 17,719.5 ± 80.9 | — | 30,089.8 | — |
| `naiveModifyStreet` | `size=8` | 3,122.1 ± 38.6 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 76,822.0 ± 283.2 | — | 193,249.8 | — |
| `naiveReadStreet` | `size=64` | 9,735.9 ± 27.1 | — | 24,737.0 | — |
| `naiveReadStreet` | `size=8` | 1,553.7 ± 6.3 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 82,991.7 ± 408.4 | — | 230,143.0 | — |
| `naiveSumPrices` | `size=64` | 10,464.0 ± 24.4 | — | 29,369.1 | — |
| `naiveSumPrices` | `size=8` | 1,629.4 ± 5.8 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 36,886.0 ± 358.5 | — | 454.6 | — |
| `nativeReadStreet` | `size=64` | 4,596.1 ± 38.1 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 789.5 ± 15.2 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 55,819.0 ± 557.9 | — | 86,256.8 | — |
| `nativeSumPrices` | `size=64` | 6,828.2 ± 120.3 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 1,019.1 ± 4.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 135,339.9 ± 1,607.9 | — | 624,394.5 | — |
| `TransformDeep` | `n=512` | 14,065.1 ± 52.2 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,537.5 ± 25.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 138,380.5 ± 3,051.3 | 160,078.0 ± 971.0 | 655,364.7 | 753,732.5 |
| `TransformExpr` | `n=512` | 17,114.9 ± 145.3 | 14,159.9 ± 306.0 | 81,825.7 | 69,585.4 |
| `TransformExpr` | `n=64` | 2,087.5 ± 23.0 | 2,454.3 ± 16.5 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 155,487.5 ± 1,295.7 | — | 786,617.2 | — |
| `UniverseDeep` | `n=512` | 19,302.1 ± 90.2 | — | 98,378.0 | — |
| `UniverseDeep` | `n=64` | 2,227.3 ± 6.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 156,153.2 ± 698.4 | 3,645,716.4 ± 10,591.9 | 786,425.6 | 4,753,865.7 |
| `UniverseExpr` | `n=512` | 18,856.0 ± 70.6 | 354,774.4 ± 6,445.5 | 98,185.9 | 483,220.2 |
| `UniverseExpr` | `n=64` | 2,191.2 ± 38.8 | 33,755.2 ± 540.9 | 12,168.0 | 46,448.7 |
| `UniverseJson` | `n=4096` | 222,881.3 ± 2,299.5 | 3,830,428.5 ± 104,799.3 | 786,474.2 | 6,490,326.9 |
| `UniverseJson` | `n=512` | 26,931.5 ± 342.5 | 387,189.6 ± 3,366.1 | 98,186.7 | 699,935.5 |
| `UniverseJson` | `n=64` | 3,051.3 ± 53.1 | 37,432.0 ± 318.6 | 12,168.1 | 73,208.7 |
| `visitorTransformDeep` | `n=4096` | 44,709.7 ± 748.5 | — | 163,888.5 | — |
| `visitorTransformDeep` | `n=512` | 4,783.1 ± 18.9 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 537.0 ± 24.9 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 70,169.4 ± 629.3 | — | 360,475.1 | — |
| `visitorTransformExpr` | `n=512` | 8,297.5 ± 169.0 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,086.4 ± 17.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 66,254.5 ± 922.0 | — | 196,712.2 | — |
| `visitorUniverseDeep` | `n=512` | 8,059.4 ± 43.0 | — | 24,632.8 | — |
| `visitorUniverseDeep` | `n=64` | 837.2 ± 10.6 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 59,567.4 ± 1,289.4 | — | 196,659.4 | — |
| `visitorUniverseExpr` | `n=512` | 7,424.7 ± 63.6 | — | 24,584.8 | — |
| `visitorUniverseExpr` | `n=64` | 874.1 ± 3.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 125,637.0 ± 13,573.4 | — | 313,706.4 | — |
| `visitorUniverseJson` | `n=512` | 12,747.9 ± 104.0 | — | 36,849.3 | — |
| `visitorUniverseJson` | `n=64` | 1,983.6 ± 195.9 | — | 5,216.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,327.1 ± 83.6 | — | 41,438.3 | — |
| `Modify_powerEach` | `size=16` | 237.2 ± 3.8 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,178.8 ± 28.5 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 110.4 ± 0.6 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 52,883.6 ± 646.9 | — | 164,398.1 | — |
| `Modify_powerEach` | `size=64` | 845.2 ± 7.6 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 56,761.8 ± 328.6 | — | 279,430.6 | — |
| `monocle_powerEach` | `size=16` | 686.6 ± 4.2 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,027.9 ± 68.8 | — | 107,347.2 | — |
| `monocle_powerEach` | `size=4` | 203.9 ± 3.5 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 198,537.0 ± 3,563.4 | — | 967,875.3 | — |
| `monocle_powerEach` | `size=64` | 2,507.9 ± 62.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,526.1 ± 21.3 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 93.5 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,376.7 ± 6.0 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 28.4 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,339.8 ± 366.7 | — | 114,780.4 | — |
| `naive_powerEach` | `size=64` | 350.9 ± 6.2 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 61,149.6 ± 486.4 | — | 210,634.0 | — |
| `Modify_nested` | `size=16` | 1,296.1 ± 5.4 | — | 4,936.0 | — |
| `Modify_nested` | `size=256` | 15,995.5 ± 174.9 | — | 53,892.7 | — |
| `Modify_nested` | `size=4` | 667.7 ± 5.1 | — | 2,408.0 | — |
| `Modify_nested` | `size=64` | 4,394.9 ± 30.9 | — | 14,808.5 | — |
| `monocle_nested` | `size=1024` | 234,927.5 ± 1,250.3 | — | 1,118,827.6 | — |
| `monocle_nested` | `size=16` | 2,874.7 ± 28.3 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 87,746.0 ± 1,211.3 | — | 430,208.7 | — |
| `monocle_nested` | `size=4` | 1,243.7 ± 12.1 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 10,282.4 ± 268.1 | — | 58,907.8 | — |
| `naive_nested` | `size=1024` | 21,889.3 ± 130.1 | — | 115,071.9 | — |
| `naive_nested` | `size=16` | 392.8 ± 4.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,335.4 ± 34.4 | — | 29,019.4 | — |
| `naive_nested` | `size=4` | 139.8 ± 1.2 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,397.7 ± 22.1 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,406.7 ± 13.7 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 27,491.8 ± 231.3 | — | 104,799.3 | — |
| `Modify_sparse` | `size=32` | 415.3 ± 7.8 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 6,798.3 ± 79.4 | — | 24,879.2 | — |
| `Modify_sparse` | `size=8` | 137.8 ± 1.1 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 4,376.9 ± 51.6 | — | 24,717.5 | — |
| `monocle_sparse` | `size=2048` | 106,335.6 ± 1,879.0 | — | 476,036.0 | — |
| `monocle_sparse` | `size=32` | 1,096.6 ± 23.3 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,087.1 ± 269.1 | — | 156,442.5 | — |
| `monocle_sparse` | `size=8` | 311.0 ± 2.6 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 330.5 ± 2.1 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,661.4 ± 37.3 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 79.8 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,419.9 ± 7.1 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.9 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.1 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.7 ± 0.0 | 2.7 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 16.6 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 29.2 ± 0.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.5 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 17.4 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 138,387.5 ± 625.0 | — | 786,297.0 | — |
| `Cata` | `-` | 72,525.1 ± 378.5 | — | 197,568.5 | — |
| `Hylo` | `-` | 78,912.9 ± 1,780.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 58,332.3 ± 923.6 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,978.8 ± 211.1 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 60,433.8 ± 702.3 | — | 328,640.4 | — |
| `handAna` | `-` | 30,556.4 ± 135.7 | — | 163,816.2 | — |
| `handCata` | `-` | 15,989.3 ± 164.3 | — | 0.1 | — |
| `handHylo` | `-` | 13,444.3 ± 441.3 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.5 ± 0.0 | 3.5 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.1 ± 0.2 | 28.5 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 22.0 ± 0.1 | 51.2 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 5.6 ± 0.0 | 5.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,820.9 ± 97.5 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 684.0 ± 12.6 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 56.7 ± 1.0 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,733.0 ± 26.5 | 30,835.3 ± 187.4 | 12,312.4 | 162,580.9 |
| `FoldPrices` | `size=64` | 305.6 ± 1.5 | 2,701.1 ± 43.9 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 41.3 ± 0.2 | 389.4 ± 13.5 | 216.0 | 2,010.7 |
| `Modify` | `size=512` | 9,201.2 ± 163.9 | 34,656.8 ± 1,044.6 | 36,897.5 | 176,913.5 |
| `Modify` | `size=64` | 1,041.4 ± 9.9 | 2,355.2 ± 47.3 | 4,640.0 | 14,448.1 |
| `Modify` | `size=8` | 110.6 ± 0.7 | 354.0 ± 9.2 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.1 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 116.7 ± 2.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 4.7 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 91.5 ± 0.2 | — | 456.0 | — |
| `handDrillGet` | `-` | 17.5 ± 0.4 | — | 120.0 | — |
| `handDrillModify` | `-` | 112.6 ± 0.8 | — | 648.0 | — |
| `handServiceGet` | `-` | 17.5 ± 0.6 | — | 120.0 | — |
| `handServiceReplace` | `-` | 99.6 ± 1.1 | — | 576.0 | — |

