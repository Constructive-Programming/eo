# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `b79627915fff94f2d668fdbb39b613721f7a9abf` · date: `2026-09-05` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.7 ± 0.1 | 9.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 33.4 ± 0.6 | 25.1 ± 0.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 164.8 ± 3.3 | — | 720.0 | — |
| `ModifyCountry` | `-` | 423.9 ± 23.5 | — | 3,234.7 | — |
| `ModifyPartner` | `-` | 525.1 ± 28.1 | — | 3,325.3 | — |
| `ReadCountry` | `-` | 174.5 ± 3.6 | — | 520.0 | — |
| `ReadPartner` | `-` | 210.1 ± 4.8 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 335.5 ± 1.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,662.6 ± 53.3 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,686.6 ± 45.5 | — | 7,536.0 | — |
| `naivePassthroughPayload` | `-` | 3,957.6 ± 32.1 | — | 10,600.1 | — |
| `naiveReadCountry` | `-` | 1,634.3 ± 16.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,769.9 ± 15.8 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 749.1 ± 11.5 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 574.6 ± 9.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 396.6 ± 3.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 420.6 ± 8.1 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,372.3 ± 38.5 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,357.9 ± 12.6 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,136.8 ± 13.5 | — | 9,394.7 | — |
| `ClickToJson` | `-` | 2,839.9 ± 42.8 | — | 3,968.0 | — |
| `WideToAvro` | `-` | 1,070.4 ± 55.7 | — | 6,696.0 | — |
| `WideToJson` | `-` | 694.8 ± 25.1 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,540.4 ± 24.2 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,700.0 ± 50.9 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,029.8 ± 33.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,880.9 ± 50.0 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 218.7 ± 1.0 | — | 880.0 | — |
| `decode_native` | `-` | 18.7 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 216.5 ± 1.1 | — | 880.0 | — |
| `encode_bridged` | `-` | 259.8 ± 14.9 | — | 1,229.3 | — |
| `encode_native` | `-` | 12.0 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 256.6 ± 14.9 | — | 1,218.7 | — |
| `fieldGet_bridged` | `-` | 96.8 ± 1.2 | — | 432.0 | — |
| `fieldGet_native` | `-` | 97.5 ± 0.8 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 426.2 ± 9.5 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 178.9 ± 6.5 | — | 613.3 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 22.4 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.3 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 23.8 ± 3.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 22.3 ± 1.6 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.6 ± 0.4 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.1 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.6 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 20.6 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 36.8 ± 0.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 18.5 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 47.1 ± 1.7 | — | 72.0 | — |
| `reuseLens6` | `-` | 130.8 ± 2.8 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 58.6 ± 0.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,015.1 ± 23.9 | 3,950.0 ± 71.9 | 14,080.7 | 14,080.6 |
| `FoldMap` | `size=64` | 373.6 ± 3.5 | 344.6 ± 2.3 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.2 ± 0.1 | 22.6 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,784.4 ± 16.9 | 2,802.3 ± 14.3 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 351.0 ± 1.3 | 354.0 ± 3.8 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.2 ± 0.1 | 45.0 ± 1.0 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.5 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.4 ± 0.2 | 9.0 ± 0.6 | 0.0 | 0.0 |
| `Get_6` | `-` | 35.6 ± 1.0 | 27.5 ± 2.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.1 | 3.9 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.0 | 3.5 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 424,147.5 ± 6,024.8 | — | 1,066,726.9 | — |
| `cModifyId` | `size=64` | 53,270.1 ± 267.5 | — | 136,269.9 | — |
| `cModifyId` | `size=8` | 8,975.5 ± 342.4 | — | 20,704.1 | — |
| `cReadId` | `size=512` | 212,340.8 ± 5,632.4 | — | 797,932.4 | — |
| `cReadId` | `size=64` | 26,717.9 ± 346.9 | — | 101,294.5 | — |
| `cReadId` | `size=8` | 4,149.4 ± 70.5 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 220,427.9 ± 16,983.7 | — | 797,934.7 | — |
| `cReadStreet` | `size=64` | 26,580.3 ± 378.3 | — | 101,294.4 | — |
| `cReadStreet` | `size=8` | 4,124.4 ± 80.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 427,943.5 ± 5,474.9 | — | 1,066,678.1 | — |
| `cReplaceId` | `size=64` | 53,023.5 ± 378.6 | — | 136,221.9 | — |
| `cReplaceId` | `size=8` | 8,872.4 ± 88.5 | — | 20,648.1 | — |
| `cSumPrices` | `size=512` | 353,224.0 ± 3,094.1 | — | 1,240,744.7 | — |
| `cSumPrices` | `size=64` | 43,955.9 ± 491.0 | — | 156,981.8 | — |
| `cSumPrices` | `size=8` | 6,387.0 ± 105.8 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 178.1 ± 6.3 | — | 0.1 | — |
| `jMiss` | `size=64` | 179.9 ± 5.2 | — | 0.0 | — |
| `jMiss` | `size=8` | 178.0 ± 7.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,815.1 ± 77.8 | — | 41,921.1 | — |
| `jModifyId` | `size=64` | 510.8 ± 9.3 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 120.4 ± 1.5 | — | 984.0 | — |
| `jReadId` | `size=512` | 36.5 ± 2.0 | — | 56.0 | — |
| `jReadId` | `size=64` | 37.7 ± 1.9 | — | 56.0 | — |
| `jReadId` | `size=8` | 36.7 ± 1.8 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 199.9 ± 9.3 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 197.4 ± 2.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 204.4 ± 8.7 | — | 144.0 | — |
| `jReplaceId` | `size=512` | 3,768.5 ± 98.6 | — | 41,889.1 | — |
| `jReplaceId` | `size=64` | 497.2 ± 10.4 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 115.0 ± 6.4 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 86,317.9 ± 1,449.6 | — | 63,663.9 | — |
| `jSumPrices` | `size=64` | 10,761.6 ± 318.1 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,492.7 ± 16.8 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 66.9 ± 5.8 | — | 312.0 | — |
| `MapDrillModify` | `-` | 42.5 ± 2.6 | — | 216.0 | — |
| `MapGet` | `-` | 2.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 57.6 ± 2.8 | — | 200.0 | — |
| `handEnvUse` | `-` | 65.3 ± 0.8 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 36.0 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 50.8 ± 1.2 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.1 | 4.4 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.1 ± 0.2 | 31.3 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.6 ± 0.1 | 3.6 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,587.4 ± 1,305.2 | — | 43,036.0 | — |
| `Fold_powerEach` | `size=256` | 3,475.7 ± 51.9 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 402.0 ± 3.4 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 87.5 ± 0.2 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 52,130.6 ± 857.6 | — | 331,132.7 | — |
| `Modify_multiFocus` | `size=256` | 12,358.1 ± 1,027.7 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,414.6 ± 10.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 229.1 ± 8.5 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 36,024.7 ± 362.3 | — | 115,177.4 | — |
| `Modify_powerEach` | `size=256` | 8,556.1 ± 85.3 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,044.4 ± 3.4 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 204.1 ± 5.2 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,387.0 ± 133.4 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,096.8 ± 14.0 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 253.7 ± 3.8 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.9 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,471.2 ± 112.2 | — | 16,129.2 | — |
| `naive_sumQty` | `size=256` | 786.6 ± 21.4 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 66.1 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 7.5 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 67.1 ± 0.5 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 173.8 ± 0.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 17.3 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 29.0 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 36.3 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.1 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.0 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 169.3 ± 1.9 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.5 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,144.2 ± 25.8 | — | 2,864.0 | — |
| `reuseUse` | `-` | 1,075.2 ± 30.1 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.0 ± 0.2 | 21.8 ± 0.2 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 58.6 ± 0.7 | 73.0 ± 0.3 | 160.0 | 304.0 |
| `Modify_6` | `-` | 144.4 ± 1.6 | 121.7 ± 0.9 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.6 ± 0.1 | 16.8 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.7 ± 0.0 | 3.8 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.8 ± 0.1 | 7.6 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 35,313.6 ± 1,129.2 | — | 97,408.3 | — |
| `ModifyNames` | `size=64` | 4,183.2 ± 15.1 | — | 12,584.1 | — |
| `ModifyNames` | `size=8` | 601.6 ± 9.1 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 121.9 ± 0.5 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 122.3 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 123.1 ± 1.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 38.3 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 38.4 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 38.3 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,825.7 ± 2,030.2 | — | 382,769.4 | — |
| `monocleModifyNames` | `size=64` | 9,970.5 ± 503.6 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,487.7 ± 9.1 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 53,710.0 ± 5,106.4 | — | 169,074.8 | — |
| `monocleModifyStreet` | `size=64` | 7,292.5 ± 65.7 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 985.6 ± 16.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 34,685.7 ± 267.6 | — | 69,791.6 | — |
| `monocleReadStreet` | `size=64` | 4,421.2 ± 13.4 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 519.1 ± 6.6 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 70,457.4 ± 1,313.0 | — | 226,301.1 | — |
| `naiveModifyNames` | `size=64` | 8,935.0 ± 25.7 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,167.8 ± 12.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,751.2 ± 1,850.6 | — | 169,062.1 | — |
| `naiveModifyStreet` | `size=64` | 6,652.3 ± 497.8 | — | 20,864.2 | — |
| `naiveModifyStreet` | `size=8` | 985.7 ± 9.1 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 34,629.2 ± 320.6 | — | 69,791.5 | — |
| `naiveReadStreet` | `size=64` | 4,060.0 ± 525.4 | — | 8,840.1 | — |
| `naiveReadStreet` | `size=8` | 513.8 ± 2.4 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 252,058.0 ± 7,443.0 | — | 609,938.0 | — |
| `Names` | `size=64` | 32,449.1 ± 876.4 | — | 79,299.4 | — |
| `Names` | `size=8` | 4,437.8 ± 59.2 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 275,141.1 ± 13,835.1 | — | 679,460.5 | — |
| `NamesIor` | `size=64` | 35,923.0 ± 1,298.7 | — | 87,685.0 | — |
| `NamesIor` | `size=8` | 4,643.4 ± 52.6 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,087.7 ± 19.4 | — | 2,720.9 | — |
| `Street` | `size=64` | 1,147.8 ± 62.1 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,078.6 ± 21.9 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,070.4 ± 12.1 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,114.4 ± 9.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,113.5 ± 31.9 | — | 2,736.0 | — |
| `directNames` | `size=512` | 256,516.1 ± 5,186.6 | — | 614,005.6 | — |
| `directNames` | `size=64` | 31,032.0 ± 238.7 | — | 77,200.4 | — |
| `directNames` | `size=8` | 4,245.2 ± 109.3 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,117.4 ± 19.6 | — | 2,728.9 | — |
| `directStreet` | `size=64` | 1,077.2 ± 24.0 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,074.1 ± 22.1 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 257,286.0 ± 12,542.2 | — | 609,901.0 | — |
| `hcursorNames` | `size=64` | 30,795.4 ± 647.1 | — | 77,792.6 | — |
| `hcursorNames` | `size=8` | 4,255.1 ± 85.6 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,158.6 ± 25.7 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,210.9 ± 49.2 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,150.7 ± 9.8 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 231,661.4 ± 2,977.1 | — | 1,121,763.7 | — |
| `monocleNames` | `size=64` | 25,302.8 ± 202.3 | — | 132,798.5 | — |
| `monocleNames` | `size=8` | 3,904.6 ± 66.2 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 185,615.9 ± 819.4 | — | 908,028.7 | — |
| `monocleStreet` | `size=64` | 22,647.6 ± 369.4 | — | 113,810.1 | — |
| `monocleStreet` | `size=8` | 3,378.8 ± 25.2 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 202,129.8 ± 3,804.4 | — | 965,271.8 | — |
| `naiveNames` | `size=64` | 24,045.3 ± 355.2 | — | 120,827.5 | — |
| `naiveNames` | `size=8` | 3,537.6 ± 10.4 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 187,212.2 ± 2,581.9 | — | 908,040.5 | — |
| `naiveStreet` | `size=64` | 22,245.9 ± 96.0 | — | 113,797.1 | — |
| `naiveStreet` | `size=8` | 3,376.5 ± 45.7 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,082.1 ± 41.7 | — | 42,027.6 | — |
| `ModifyStreet` | `size=64` | 710.9 ± 19.0 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 312.9 ± 15.1 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 203.6 ± 1.7 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 205.5 ± 9.3 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 202.6 ± 1.2 | — | 90.7 | — |
| `SumPrices` | `size=512` | 88,373.6 ± 515.0 | — | 63,715.0 | — |
| `SumPrices` | `size=64` | 10,415.2 ± 87.0 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,437.5 ± 17.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 167,534.1 ± 2,307.8 | — | 333,571.9 | — |
| `monocleModifyStreet` | `size=64` | 20,003.6 ± 70.2 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,304.0 ± 14.3 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 93,830.8 ± 567.4 | — | 193,232.3 | — |
| `monocleReadStreet` | `size=64` | 11,927.4 ± 92.8 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,903.9 ± 22.2 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 443,139.8 ± 9,173.2 | — | 1,190,765.5 | — |
| `monocleSumPrices` | `size=64` | 17,105.6 ± 954.1 | — | 47,385.7 | — |
| `monocleSumPrices` | `size=8` | 2,568.7 ± 15.4 | — | 6,640.1 | — |
| `naiveModifyStreet` | `size=512` | 167,682.2 ± 3,225.2 | — | 333,573.5 | — |
| `naiveModifyStreet` | `size=64` | 20,023.3 ± 146.1 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,317.1 ± 45.2 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 94,106.2 ± 1,398.9 | — | 193,232.6 | — |
| `naiveReadStreet` | `size=64` | 11,953.9 ± 191.6 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,908.2 ± 19.1 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 98,393.7 ± 937.2 | — | 230,124.2 | — |
| `naiveSumPrices` | `size=64` | 12,451.7 ± 99.6 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,977.9 ± 18.3 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 34,918.8 ± 382.2 | — | 453.0 | — |
| `nativeReadStreet` | `size=64` | 4,474.0 ± 48.5 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 851.9 ± 22.9 | — | 413.4 | — |
| `nativeSumPrices` | `size=512` | 64,449.4 ± 1,040.4 | — | 86,273.0 | — |
| `nativeSumPrices` | `size=64` | 8,098.6 ± 157.9 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,206.4 ± 15.0 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 124,397.7 ± 835.8 | — | 624,386.5 | — |
| `TransformDeep` | `n=512` | 11,947.8 ± 173.7 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,446.4 ± 2.6 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 131,374.1 ± 2,075.1 | 170,054.6 ± 622.8 | 655,359.6 | 753,739.8 |
| `TransformExpr` | `n=512` | 16,479.2 ± 226.9 | 15,556.1 ± 44.0 | 81,825.7 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,052.9 ± 15.2 | 2,600.3 ± 7.7 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 104,465.1 ± 5,777.2 | — | 786,580.0 | — |
| `UniverseDeep` | `n=512` | 15,650.4 ± 93.9 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,919.4 ± 11.7 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 100,949.0 ± 2,845.3 | 2,809,478.4 ± 226,192.9 | 786,385.5 | 4,687,721.8 |
| `UniverseExpr` | `n=512` | 15,151.0 ± 89.1 | 178,250.9 ± 10,117.6 | 98,185.5 | 475,010.4 |
| `UniverseExpr` | `n=64` | 1,861.1 ± 17.3 | 14,882.9 ± 78.8 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 233,822.5 ± 1,681.6 | 3,061,344.8 ± 221,875.5 | 786,482.2 | 6,882,936.9 |
| `UniverseJson` | `n=512` | 27,573.2 ± 208.2 | 204,095.8 ± 2,530.9 | 98,186.8 | 699,916.8 |
| `UniverseJson` | `n=64` | 3,367.1 ± 24.8 | 19,433.6 ± 365.9 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,035.3 ± 298.3 | — | 163,886.6 | — |
| `visitorTransformDeep` | `n=512` | 5,005.8 ± 150.4 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 433.7 ± 2.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 65,432.5 ± 419.4 | — | 360,471.6 | — |
| `visitorTransformExpr` | `n=512` | 7,650.0 ± 46.5 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 993.4 ± 7.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,267.4 ± 818.3 | — | 196,707.1 | — |
| `visitorUniverseDeep` | `n=512` | 7,257.3 ± 58.5 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 832.9 ± 2.5 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 54,057.2 ± 700.3 | — | 196,655.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,769.6 ± 38.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 824.4 ± 6.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 183,697.5 ± 11,295.1 | — | 447,941.7 | — |
| `visitorUniverseJson` | `n=512` | 21,284.0 ± 394.7 | — | 42,314.2 | — |
| `visitorUniverseJson` | `n=64` | 2,120.5 ± 60.3 | — | 4,424.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,967.0 ± 234.3 | — | 41,414.1 | — |
| `Modify_powerEach` | `size=16` | 268.3 ± 3.9 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,164.4 ± 32.3 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 128.0 ± 2.1 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 54,415.3 ± 1,876.4 | — | 164,376.6 | — |
| `Modify_powerEach` | `size=64` | 825.5 ± 5.5 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 59,392.3 ± 805.3 | — | 279,432.4 | — |
| `monocle_powerEach` | `size=16` | 646.3 ± 8.6 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 22,090.5 ± 164.0 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 245.7 ± 23.3 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 186,878.0 ± 1,681.7 | — | 967,856.3 | — |
| `monocle_powerEach` | `size=64` | 2,148.7 ± 43.4 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,240.8 ± 39.1 | — | 28,730.5 | — |
| `naive_powerEach` | `size=16` | 99.7 ± 1.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,631.5 ± 3.7 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.6 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 20,681.6 ± 79.5 | — | 114,777.7 | — |
| `naive_powerEach` | `size=64` | 399.5 ± 3.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 75,930.5 ± 1,331.8 | — | 210,612.9 | — |
| `Modify_nested` | `size=16` | 1,528.2 ± 10.1 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 18,289.6 ± 156.7 | — | 53,875.4 | — |
| `Modify_nested` | `size=4` | 700.2 ± 6.1 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,851.1 ± 40.4 | — | 14,616.6 | — |
| `monocle_nested` | `size=1024` | 249,752.6 ± 1,382.2 | — | 1,118,854.7 | — |
| `monocle_nested` | `size=16` | 2,932.4 ± 40.5 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 94,941.9 ± 2,489.8 | — | 430,210.1 | — |
| `monocle_nested` | `size=4` | 1,392.4 ± 52.1 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,814.2 ± 208.2 | — | 58,907.6 | — |
| `naive_nested` | `size=1024` | 20,113.7 ± 145.0 | — | 115,068.6 | — |
| `naive_nested` | `size=16` | 376.2 ± 7.9 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,397.8 ± 210.6 | — | 29,019.4 | — |
| `naive_nested` | `size=4` | 138.6 ± 14.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,337.4 ± 15.4 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,374.7 ± 26.8 | — | 4,840.1 | — |
| `Modify_sparse` | `size=2048` | 24,111.4 ± 317.4 | — | 104,704.9 | — |
| `Modify_sparse` | `size=32` | 358.8 ± 2.3 | — | 1,384.0 | — |
| `Modify_sparse` | `size=512` | 5,912.5 ± 56.8 | — | 24,809.6 | — |
| `Modify_sparse` | `size=8` | 128.0 ± 0.8 | — | 520.0 | — |
| `monocle_sparse` | `size=128` | 3,950.6 ± 121.7 | — | 27,800.2 | — |
| `monocle_sparse` | `size=2048` | 109,164.6 ± 1,638.0 | — | 523,148.2 | — |
| `monocle_sparse` | `size=32` | 1,103.3 ± 25.3 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 34,721.1 ± 890.9 | — | 166,686.9 | — |
| `monocle_sparse` | `size=8` | 339.2 ± 2.5 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 327.1 ± 2.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,147.0 ± 35.0 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 83.7 ± 0.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,201.1 ± 4.5 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.5 ± 0.3 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.6 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 20.0 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.6 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.3 ± 0.2 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 106,518.2 ± 1,005.7 | — | 589,712.8 | — |
| `Cata` | `-` | 78,851.5 ± 1,230.6 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,663.7 ± 713.5 | — | 295,848.6 | — |
| `drosteAna` | `-` | 48,803.1 ± 958.1 | — | 327,632.3 | — |
| `drosteCata` | `-` | 39,592.7 ± 458.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 106,542.4 ± 64,330.0 | — | 328,640.8 | — |
| `handAna` | `-` | 20,535.0 ± 514.7 | — | 163,816.1 | — |
| `handCata` | `-` | 13,192.5 ± 158.4 | — | 0.1 | — |
| `handHylo` | `-` | 9,525.8 ± 27.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.5 ± 0.0 | 2.5 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.9 ± 0.1 | 28.5 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.6 ± 0.2 | 65.4 ± 0.6 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.1 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,548.9 ± 159.8 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 541.9 ± 23.1 | — | 2,648.0 | — |
| `FoldNested` | `size=8` | 57.0 ± 3.0 | — | 392.0 | — |
| `FoldPrices` | `size=512` | 2,828.9 ± 8.7 | 32,035.6 ± 273.8 | 12,312.4 | 162,581.1 |
| `FoldPrices` | `size=64` | 354.4 ± 3.0 | 2,190.7 ± 67.3 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 44.1 ± 0.7 | 338.2 ± 12.7 | 216.0 | 2,010.7 |
| `Modify` | `size=512` | 7,632.6 ± 319.6 | 33,939.8 ± 405.9 | 36,897.2 | 176,925.3 |
| `Modify` | `size=64` | 869.0 ± 2.4 | 1,779.3 ± 5.8 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 98.0 ± 1.4 | 295.0 ± 6.4 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 12.1 ± 0.2 | — | 0.0 | — |
| `DrillModify` | `-` | 115.0 ± 1.1 | — | 528.0 | — |
| `ServiceGet` | `-` | 5.2 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 82.8 ± 0.7 | — | 456.0 | — |
| `handDrillGet` | `-` | 14.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 107.8 ± 0.9 | — | 648.0 | — |
| `handServiceGet` | `-` | 15.0 ± 0.0 | — | 120.0 | — |
| `handServiceReplace` | `-` | 90.3 ± 1.0 | — | 576.0 | — |

