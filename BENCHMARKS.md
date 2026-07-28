# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `9910dce624d27c7575eeb4480ac0d44f1659b873` · date: `2026-07-28` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.1 ± 0.0 | 10.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.9 ± 0.4 | 24.3 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.4 ± 0.9 | — | 720.0 | — |
| `ModifyCountry` | `-` | 339.0 ± 25.3 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 394.6 ± 9.1 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 185.9 ± 10.0 | — | 520.0 | — |
| `ReadPartner` | `-` | 217.3 ± 7.4 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 329.6 ± 2.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,765.9 ± 14.0 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,758.0 ± 77.7 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,347.3 ± 86.4 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,752.9 ± 36.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,780.8 ± 64.6 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 760.6 ± 14.8 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 545.4 ± 6.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 416.9 ± 8.1 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 432.9 ± 9.1 | — | 1,541.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,310.8 ± 6.8 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,301.1 ± 30.0 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,551.2 ± 25.7 | — | 9,346.7 | — |
| `ClickToJson` | `-` | 2,992.3 ± 32.9 | — | 4,016.0 | — |
| `WideToAvro` | `-` | 841.6 ± 25.6 | — | 6,552.0 | — |
| `WideToJson` | `-` | 632.1 ± 35.1 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,644.4 ± 313.6 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,889.0 ± 61.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 995.2 ± 9.5 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,956.9 ± 74.3 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 223.0 ± 0.8 | — | 984.0 | — |
| `decode_native` | `-` | 20.2 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 218.2 ± 2.3 | — | 984.0 | — |
| `encode_bridged` | `-` | 233.3 ± 14.1 | — | 1,277.3 | — |
| `encode_native` | `-` | 15.9 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 238.2 ± 9.3 | — | 1,293.3 | — |
| `fieldGet_bridged` | `-` | 98.8 ± 1.4 | — | 432.0 | — |
| `fieldGet_native` | `-` | 98.7 ± 0.5 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 394.0 ± 8.5 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 179.9 ± 1.4 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.8 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.8 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.0 ± 0.2 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.1 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 33.8 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 39.3 ± 0.8 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.3 | — | 72.0 | — |
| `buildLens3` | `-` | 22.3 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 43.8 ± 0.9 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 22.6 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.4 | — | 40.0 | — |
| `reuseLens3` | `-` | 48.9 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 138.7 ± 5.2 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 63.9 ± 0.8 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 5,036.8 ± 16.0 | 4,753.0 ± 54.0 | 14,080.8 | 14,080.8 |
| `FoldMap` | `size=64` | 325.1 ± 0.6 | 314.2 ± 6.3 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.4 ± 0.0 | 22.0 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,159.9 ± 151.7 | 3,135.4 ± 16.5 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 374.5 ± 8.1 | 374.3 ± 4.8 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 48.2 ± 0.4 | 48.5 ± 0.8 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.3 ± 0.0 | 8.8 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.2 ± 0.1 | 27.5 ± 0.5 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.1 | 4.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.0 | 3.3 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 434,077.6 ± 1,848.8 | — | 1,072,932.0 | — |
| `cModifyId` | `size=64` | 56,769.8 ± 1,170.2 | — | 136,327.0 | — |
| `cModifyId` | `size=8` | 9,798.7 ± 145.8 | — | 21,016.1 | — |
| `cReadId` | `size=512` | 229,308.2 ± 1,753.6 | — | 804,121.2 | — |
| `cReadId` | `size=64` | 28,617.8 ± 153.2 | — | 101,318.0 | — |
| `cReadId` | `size=8` | 4,569.9 ± 25.2 | — | 15,608.0 | — |
| `cReadStreet` | `size=512` | 226,462.4 ± 907.2 | — | 804,112.4 | — |
| `cReadStreet` | `size=64` | 28,692.3 ± 280.5 | — | 101,325.9 | — |
| `cReadStreet` | `size=8` | 4,658.4 ± 95.6 | — | 15,608.0 | — |
| `cReplaceId` | `size=512` | 436,340.6 ± 2,913.7 | — | 1,072,854.3 | — |
| `cReplaceId` | `size=64` | 55,949.6 ± 191.4 | — | 136,254.6 | — |
| `cReplaceId` | `size=8` | 9,607.9 ± 246.2 | — | 20,928.1 | — |
| `cSumPrices` | `size=512` | 361,612.5 ± 2,865.0 | — | 1,250,958.8 | — |
| `cSumPrices` | `size=64` | 44,975.6 ± 330.3 | — | 157,503.5 | — |
| `cSumPrices` | `size=8` | 6,624.8 ± 44.2 | — | 22,784.1 | — |
| `jMiss` | `size=512` | 198.1 ± 8.0 | — | 0.1 | — |
| `jMiss` | `size=64` | 192.9 ± 0.5 | — | 0.0 | — |
| `jMiss` | `size=8` | 192.9 ± 0.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,952.4 ± 70.4 | — | 41,928.8 | — |
| `jModifyId` | `size=64` | 342.5 ± 3.6 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 104.9 ± 1.4 | — | 992.0 | — |
| `jReadId` | `size=512` | 40.5 ± 4.5 | — | 64.0 | — |
| `jReadId` | `size=64` | 35.5 ± 0.1 | — | 48.0 | — |
| `jReadId` | `size=8` | 36.1 ± 0.2 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 213.9 ± 2.1 | — | 152.1 | — |
| `jReadStreet` | `size=64` | 209.8 ± 0.4 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 211.2 ± 2.2 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 2,890.4 ± 34.8 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 339.5 ± 4.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 100.2 ± 0.4 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 88,496.0 ± 1,129.5 | — | 63,663.7 | — |
| `jSumPrices` | `size=64` | 10,929.6 ± 51.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,470.7 ± 7.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 66.7 ± 0.4 | — | 312.0 | — |
| `MapDrillModify` | `-` | 45.9 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.5 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 53.3 ± 0.2 | — | 200.0 | — |
| `handEnvUse` | `-` | 66.0 ± 0.8 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 39.5 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 51.1 ± 0.6 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.1 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 39.2 ± 0.3 | 32.9 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.6 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 18,152.1 ± 2,185.7 | — | 43,036.7 | — |
| `Fold_powerEach` | `size=256` | 3,905.4 ± 109.3 | — | 9,240.3 | — |
| `Fold_powerEach` | `size=32` | 491.1 ± 5.2 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 80.4 ± 1.4 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 54,262.4 ± 937.8 | — | 380,269.5 | — |
| `Modify_multiFocus` | `size=256` | 13,397.9 ± 131.2 | — | 88,422.3 | — |
| `Modify_multiFocus` | `size=32` | 1,552.4 ± 5.3 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 226.3 ± 0.5 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 37,948.7 ± 649.8 | — | 115,201.8 | — |
| `Modify_powerEach` | `size=256` | 9,049.7 ± 83.6 | — | 26,104.6 | — |
| `Modify_powerEach` | `size=32` | 949.8 ± 145.0 | — | 3,208.0 | — |
| `Modify_powerEach` | `size=4` | 187.3 ± 14.3 | — | 856.0 | — |
| `naive_listMap` | `size=1024` | 9,467.9 ± 46.6 | — | 65,578.5 | — |
| `naive_listMap` | `size=256` | 2,276.8 ± 11.6 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 265.0 ± 0.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 35.5 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 5,152.2 ± 57.4 | — | 16,129.3 | — |
| `naive_sumQty` | `size=256` | 959.2 ± 35.8 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 78.0 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.6 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.6 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 199.8 ± 23.4 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 16.3 ± 0.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.1 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 42.0 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.2 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.0 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 165.8 ± 1.3 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 50.0 ± 1.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,075.6 ± 34.2 | — | 2,816.0 | — |
| `reuseUse` | `-` | 1,028.3 ± 25.3 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.8 ± 0.1 | 23.5 ± 1.2 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 64.7 ± 1.9 | 67.5 ± 1.8 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.6 ± 0.4 | 109.3 ± 0.6 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 21.3 ± 0.1 | 20.4 ± 0.2 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.2 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.9 ± 0.2 | 7.5 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 39,730.6 ± 543.4 | — | 97,430.9 | — |
| `ModifyNames` | `size=64` | 4,979.4 ± 19.5 | — | 12,584.2 | — |
| `ModifyNames` | `size=8` | 672.8 ± 5.4 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 146.1 ± 3.7 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 144.1 ± 1.1 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 144.4 ± 2.2 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 42.7 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 42.7 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 42.9 ± 0.4 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,302.9 ± 244.8 | — | 382,771.8 | — |
| `monocleModifyNames` | `size=64` | 10,579.6 ± 516.0 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,473.0 ± 59.6 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 58,580.8 ± 155.8 | — | 169,086.2 | — |
| `monocleModifyStreet` | `size=64` | 7,586.8 ± 45.3 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 975.5 ± 6.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,012.8 ± 86.5 | — | 69,792.1 | — |
| `monocleReadStreet` | `size=64` | 4,645.6 ± 20.7 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 542.7 ± 5.0 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 74,272.9 ± 1,099.7 | — | 226,301.4 | — |
| `naiveModifyNames` | `size=64` | 9,415.0 ± 77.6 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,161.0 ± 30.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 59,101.6 ± 1,105.6 | — | 169,063.7 | — |
| `naiveModifyStreet` | `size=64` | 7,560.5 ± 25.1 | — | 20,880.3 | — |
| `naiveModifyStreet` | `size=8` | 986.9 ± 13.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,388.0 ± 90.6 | — | 69,792.5 | — |
| `naiveReadStreet` | `size=64` | 4,678.5 ± 10.7 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 544.1 ± 1.0 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 242,076.3 ± 8,603.5 | — | 614,031.7 | — |
| `Names` | `size=64` | 30,106.1 ± 167.8 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,141.4 ± 21.8 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 244,131.6 ± 7,634.6 | — | 675,329.4 | — |
| `NamesIor` | `size=64` | 32,374.3 ± 353.0 | — | 87,532.5 | — |
| `NamesIor` | `size=8` | 4,211.4 ± 13.4 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,030.8 ± 22.4 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,043.5 ± 45.2 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,027.7 ± 9.5 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,023.2 ± 15.1 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,018.2 ± 12.3 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,019.9 ± 11.2 | — | 2,736.0 | — |
| `directNames` | `size=512` | 243,835.3 ± 4,702.1 | — | 613,992.0 | — |
| `directNames` | `size=64` | 30,334.7 ± 541.6 | — | 77,740.3 | — |
| `directNames` | `size=8` | 4,026.9 ± 104.3 | — | 10,632.1 | — |
| `directStreet` | `size=512` | 1,010.5 ± 2.7 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,005.7 ± 3.4 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,006.3 ± 8.3 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 244,857.1 ± 3,838.1 | — | 613,992.8 | — |
| `hcursorNames` | `size=64` | 29,921.2 ± 283.7 | — | 77,787.7 | — |
| `hcursorNames` | `size=8` | 3,985.6 ± 81.6 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,069.1 ± 17.6 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,066.4 ± 13.4 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,079.7 ± 10.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 233,147.0 ± 2,225.8 | — | 1,121,764.6 | — |
| `monocleNames` | `size=64` | 25,599.7 ± 112.9 | — | 132,774.8 | — |
| `monocleNames` | `size=8` | 3,786.9 ± 29.7 | — | 19,456.1 | — |
| `monocleStreet` | `size=512` | 189,432.4 ± 7,503.9 | — | 908,031.3 | — |
| `monocleStreet` | `size=64` | 22,285.4 ± 38.6 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,267.4 ± 27.0 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 205,666.5 ± 6,839.6 | — | 965,274.2 | — |
| `naiveNames` | `size=64` | 24,136.5 ± 57.0 | — | 120,826.2 | — |
| `naiveNames` | `size=8` | 3,468.4 ± 7.4 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 189,199.6 ± 6,456.5 | — | 908,031.1 | — |
| `naiveStreet` | `size=64` | 22,237.5 ± 96.1 | — | 113,791.3 | — |
| `naiveStreet` | `size=8` | 3,261.8 ± 9.5 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,233.5 ± 40.1 | — | 42,026.9 | — |
| `ModifyStreet` | `size=64` | 542.1 ± 4.6 | — | 5,440.1 | — |
| `ModifyStreet` | `size=8` | 306.6 ± 3.3 | — | 1,088.0 | — |
| `ReadStreet` | `size=512` | 211.8 ± 0.7 | — | 114.9 | — |
| `ReadStreet` | `size=64` | 212.9 ± 2.2 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 211.8 ± 0.8 | — | 128.0 | — |
| `SumPrices` | `size=512` | 88,713.9 ± 455.7 | — | 63,721.1 | — |
| `SumPrices` | `size=64` | 10,871.4 ± 55.2 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,543.7 ± 118.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 172,111.7 ± 480.4 | — | 333,581.8 | — |
| `monocleModifyStreet` | `size=64` | 21,678.1 ± 78.9 | — | 30,114.2 | — |
| `monocleModifyStreet` | `size=8` | 3,507.6 ± 23.5 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 98,697.2 ± 605.6 | — | 193,268.5 | — |
| `monocleReadStreet` | `size=64` | 12,438.7 ± 36.5 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,892.3 ± 12.7 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 472,491.1 ± 3,899.5 | — | 1,190,821.4 | — |
| `monocleSumPrices` | `size=64` | 16,974.9 ± 85.4 | — | 47,425.7 | — |
| `monocleSumPrices` | `size=8` | 2,724.6 ± 24.5 | — | 6,696.1 | — |
| `naiveModifyStreet` | `size=512` | 173,155.2 ± 4,939.3 | — | 333,608.7 | — |
| `naiveModifyStreet` | `size=64` | 21,590.3 ± 108.3 | — | 30,090.2 | — |
| `naiveModifyStreet` | `size=8` | 3,497.0 ± 21.0 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 98,751.7 ± 516.2 | — | 193,268.5 | — |
| `naiveReadStreet` | `size=64` | 15,607.9 ± 4,911.4 | — | 24,737.6 | — |
| `naiveReadStreet` | `size=8` | 1,897.5 ± 57.6 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 103,284.2 ± 430.6 | — | 230,160.4 | — |
| `naiveSumPrices` | `size=64` | 13,213.1 ± 211.1 | — | 29,369.4 | — |
| `naiveSumPrices` | `size=8` | 1,977.0 ± 10.8 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 42,250.5 ± 1,502.9 | — | 459.1 | — |
| `nativeReadStreet` | `size=64` | 5,198.0 ± 228.9 | — | 424.6 | — |
| `nativeReadStreet` | `size=8` | 866.0 ± 13.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 68,427.6 ± 511.2 | — | 86,273.5 | — |
| `nativeSumPrices` | `size=64` | 8,505.9 ± 68.2 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,240.7 ± 4.4 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,868.8 ± 654.8 | — | 624,393.4 | — |
| `TransformDeep` | `n=512` | 13,444.5 ± 68.6 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,543.8 ± 8.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 156,899.7 ± 1,812.0 | 184,565.5 ± 454.6 | 655,378.2 | 753,750.3 |
| `TransformExpr` | `n=512` | 19,378.1 ± 79.4 | 16,673.1 ± 107.0 | 81,826.0 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,364.0 ± 37.6 | 2,838.1 ± 19.6 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 117,331.5 ± 4,905.7 | — | 786,589.4 | — |
| `UniverseDeep` | `n=512` | 16,267.9 ± 50.8 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,995.8 ± 31.1 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 111,019.6 ± 2,365.5 | 1,828,211.7 ± 16,522.7 | 786,392.8 | 4,752,545.6 |
| `UniverseExpr` | `n=512` | 15,817.3 ± 106.4 | 183,648.4 ± 6,556.9 | 98,185.6 | 483,202.7 |
| `UniverseExpr` | `n=64` | 1,922.6 ± 3.4 | 16,932.1 ± 614.9 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 242,603.0 ± 3,178.4 | 2,047,367.1 ± 74,829.7 | 786,488.5 | 6,489,032.9 |
| `UniverseJson` | `n=512` | 27,949.9 ± 214.4 | 211,760.0 ± 4,703.6 | 98,186.9 | 699,917.6 |
| `UniverseJson` | `n=64` | 3,356.8 ± 32.0 | 20,704.5 ± 275.6 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 44,725.4 ± 161.7 | — | 163,888.5 | — |
| `visitorTransformDeep` | `n=512` | 4,247.9 ± 174.5 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 451.9 ± 2.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 72,746.5 ± 173.4 | — | 360,476.9 | — |
| `visitorTransformExpr` | `n=512` | 9,002.8 ± 73.4 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,121.0 ± 1.8 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,253.8 ± 449.2 | — | 196,707.1 | — |
| `visitorUniverseDeep` | `n=512` | 7,317.4 ± 49.6 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 830.1 ± 6.4 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,368.9 ± 97.6 | — | 196,656.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,921.6 ± 19.7 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 828.3 ± 21.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 130,349.0 ± 3,755.1 | — | 294,990.9 | — |
| `visitorUniverseJson` | `n=512` | 15,176.3 ± 1,444.3 | — | 36,849.6 | — |
| `visitorUniverseJson` | `n=64` | 2,000.5 ± 21.3 | — | 4,592.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 15,239.8 ± 88.6 | — | 41,439.2 | — |
| `Modify_powerEach` | `size=16` | 300.4 ± 1.8 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,633.4 ± 31.5 | — | 10,712.6 | — |
| `Modify_powerEach` | `size=4` | 136.6 ± 1.3 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 59,494.3 ± 439.5 | — | 164,408.9 | — |
| `Modify_powerEach` | `size=64` | 948.3 ± 4.9 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 58,851.7 ± 2,135.6 | — | 279,432.1 | — |
| `monocle_powerEach` | `size=16` | 580.7 ± 4.8 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,441.9 ± 161.9 | — | 107,339.3 | — |
| `monocle_powerEach` | `size=4` | 243.6 ± 15.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 183,475.1 ± 3,458.6 | — | 967,850.8 | — |
| `monocle_powerEach` | `size=64` | 2,124.6 ± 21.0 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,898.7 ± 164.4 | — | 28,730.8 | — |
| `naive_powerEach` | `size=16` | 111.5 ± 3.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,689.3 ± 2.7 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 28.5 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,262.7 ± 91.4 | — | 114,782.1 | — |
| `naive_powerEach` | `size=64` | 425.1 ± 0.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 65,152.4 ± 985.7 | — | 210,646.6 | — |
| `Modify_nested` | `size=16` | 1,567.9 ± 12.8 | — | 4,912.1 | — |
| `Modify_nested` | `size=256` | 18,792.8 ± 1,368.2 | — | 53,915.8 | — |
| `Modify_nested` | `size=4` | 728.2 ± 3.8 | — | 2,408.0 | — |
| `Modify_nested` | `size=64` | 4,966.0 ± 53.7 | — | 14,760.6 | — |
| `monocle_nested` | `size=1024` | 252,744.2 ± 6,702.3 | — | 1,118,860.1 | — |
| `monocle_nested` | `size=16` | 2,532.9 ± 38.0 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 94,058.8 ± 6,438.1 | — | 430,210.8 | — |
| `monocle_nested` | `size=4` | 1,231.8 ± 124.2 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,702.2 ± 156.9 | — | 58,912.9 | — |
| `naive_nested` | `size=1024` | 22,496.6 ± 610.6 | — | 115,073.0 | — |
| `naive_nested` | `size=16` | 415.0 ± 8.9 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,279.6 ± 108.3 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 145.6 ± 3.7 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,476.1 ± 19.7 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,615.1 ± 1.9 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,701.9 ± 413.9 | — | 104,803.1 | — |
| `Modify_sparse` | `size=32` | 540.0 ± 2.1 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,495.1 ± 13.3 | — | 24,906.1 | — |
| `Modify_sparse` | `size=8` | 173.8 ± 0.4 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,846.0 ± 49.2 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 96,269.2 ± 779.5 | — | 476,031.0 | — |
| `monocle_sparse` | `size=32` | 984.5 ± 8.5 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 32,393.3 ± 485.3 | — | 156,443.2 | — |
| `monocle_sparse` | `size=8` | 275.9 ± 1.2 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 323.2 ± 0.6 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,556.4 ± 26.1 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 83.7 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,291.4 ± 8.8 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 26.0 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.4 ± 0.3 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 38.0 ± 1.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.9 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.4 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 121,646.5 ± 717.7 | — | 786,296.9 | — |
| `Cata` | `-` | 84,295.4 ± 520.8 | — | 197,568.6 | — |
| `Hylo` | `-` | 88,890.9 ± 1,341.8 | — | 295,848.6 | — |
| `drosteAna` | `-` | 53,971.9 ± 144.3 | — | 327,632.4 | — |
| `drosteCata` | `-` | 47,231.8 ± 1,391.4 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 62,815.6 ± 483.0 | — | 328,640.4 | — |
| `handAna` | `-` | 21,826.9 ± 383.9 | — | 163,816.2 | — |
| `handCata` | `-` | 12,962.9 ± 51.0 | — | 0.1 | — |
| `handHylo` | `-` | 11,811.5 ± 507.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 0.1 | 26.6 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.2 ± 0.1 | 52.7 ± 0.9 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,478.2 ± 65.4 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 603.0 ± 14.7 | — | 2,648.0 | — |
| `FoldNested` | `size=8` | 60.6 ± 4.5 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 3,077.9 ± 6.8 | 30,768.7 ± 167.1 | 12,312.5 | 162,580.9 |
| `FoldPrices` | `size=64` | 373.9 ± 1.7 | 2,292.3 ± 3.8 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 48.4 ± 0.3 | 291.9 ± 0.9 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 9,168.3 ± 109.2 | 34,073.8 ± 1,211.1 | 36,897.5 | 176,925.3 |
| `Modify` | `size=64` | 967.7 ± 5.3 | 1,803.6 ± 13.9 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 114.3 ± 0.8 | 243.4 ± 1.8 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 13.5 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 123.3 ± 1.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 6.1 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 86.1 ± 0.3 | — | 456.0 | — |
| `handDrillGet` | `-` | 13.4 ± 0.0 | — | 120.0 | — |
| `handDrillModify` | `-` | 109.9 ± 1.1 | — | 648.0 | — |
| `handServiceGet` | `-` | 13.2 ± 0.4 | — | 120.0 | — |
| `handServiceReplace` | `-` | 89.8 ± 0.6 | — | 576.0 | — |

