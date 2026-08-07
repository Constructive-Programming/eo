# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `c5e5d2d70fbbe311b040f14a6f2f412d519b486c` · date: `2026-08-07` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.3 ± 0.0 | 10.6 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.3 ± 0.2 | 26.0 ± 1.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.0 ± 2.0 | — | 720.0 | — |
| `ModifyCountry` | `-` | 328.7 ± 9.9 | — | 3,200.0 | — |
| `ModifyPartner` | `-` | 390.2 ± 4.8 | — | 3,256.0 | — |
| `ReadCountry` | `-` | 181.9 ± 5.5 | — | 520.0 | — |
| `ReadPartner` | `-` | 211.2 ± 3.4 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 334.0 ± 6.0 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,682.7 ± 12.9 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,754.1 ± 22.3 | — | 7,536.0 | — |
| `naivePassthroughPayload` | `-` | 4,074.6 ± 144.3 | — | 10,600.1 | — |
| `naiveReadCountry` | `-` | 1,643.5 ± 19.6 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,731.1 ± 38.6 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 736.2 ± 10.0 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 533.0 ± 6.1 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 410.5 ± 2.2 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 434.2 ± 2.8 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,370.7 ± 54.5 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,370.3 ± 8.2 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,190.7 ± 24.6 | — | 9,389.4 | — |
| `ClickToJson` | `-` | 2,821.7 ± 37.6 | — | 3,978.7 | — |
| `WideToAvro` | `-` | 774.2 ± 13.6 | — | 6,584.0 | — |
| `WideToJson` | `-` | 611.8 ± 22.5 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,404.2 ± 7.6 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,688.9 ± 64.3 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 944.2 ± 5.3 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,865.3 ± 46.7 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 217.9 ± 3.0 | — | 880.0 | — |
| `decode_native` | `-` | 19.7 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 210.0 ± 0.4 | — | 880.0 | — |
| `encode_bridged` | `-` | 233.4 ± 8.8 | — | 1,234.7 | — |
| `encode_native` | `-` | 13.0 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 233.9 ± 5.7 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 95.6 ± 0.9 | — | 432.0 | — |
| `fieldGet_native` | `-` | 97.3 ± 2.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 384.0 ± 4.5 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 172.4 ± 2.5 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.9 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.0 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.7 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.7 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 31.6 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 37.7 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.4 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.9 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.8 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 40.4 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.5 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 17.9 ± 0.4 | — | 40.0 | — |
| `reuseLens3` | `-` | 49.3 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 135.2 ± 0.3 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 62.9 ± 0.5 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,345.7 ± 29.2 | 4,340.6 ± 123.7 | 14,080.7 | 14,080.7 |
| `FoldMap` | `size=64` | 391.2 ± 2.8 | 382.0 ± 1.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.0 ± 0.1 | 21.9 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,118.6 ± 148.4 | 3,090.0 ± 32.9 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 371.9 ± 3.8 | 370.2 ± 0.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 47.9 ± 0.0 | 47.9 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 1.0 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.3 ± 0.0 | 9.4 ± 0.5 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.2 ± 0.2 | 25.5 ± 0.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 3.9 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.3 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 433,558.9 ± 3,095.5 | — | 1,066,715.4 | — |
| `cModifyId` | `size=64` | 56,179.6 ± 380.9 | — | 136,270.6 | — |
| `cModifyId` | `size=8` | 9,212.5 ± 47.5 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 218,643.8 ± 566.7 | — | 797,934.2 | — |
| `cReadId` | `size=64` | 28,323.9 ± 562.3 | — | 101,293.6 | — |
| `cReadId` | `size=8` | 4,243.2 ± 43.9 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 219,504.2 ± 2,196.2 | — | 797,934.4 | — |
| `cReadStreet` | `size=64` | 27,786.6 ± 184.1 | — | 101,293.4 | — |
| `cReadStreet` | `size=8` | 4,271.9 ± 46.1 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 455,135.9 ± 36,692.7 | — | 1,066,679.6 | — |
| `cReplaceId` | `size=64` | 55,694.3 ± 223.8 | — | 136,214.5 | — |
| `cReplaceId` | `size=8` | 9,372.4 ± 157.6 | — | 20,640.1 | — |
| `cSumPrices` | `size=512` | 354,744.3 ± 1,011.9 | — | 1,240,741.0 | — |
| `cSumPrices` | `size=64` | 44,704.3 ± 1,347.6 | — | 157,033.5 | — |
| `cSumPrices` | `size=8` | 6,402.4 ± 56.3 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 189.4 ± 4.7 | — | 0.1 | — |
| `jMiss` | `size=64` | 187.3 ± 5.3 | — | 0.0 | — |
| `jMiss` | `size=8` | 189.3 ± 4.7 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,781.0 ± 36.3 | — | 41,920.8 | — |
| `jModifyId` | `size=64` | 331.0 ± 2.4 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 102.9 ± 2.7 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.4 ± 2.5 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.9 ± 0.4 | — | 48.0 | — |
| `jReadId` | `size=8` | 39.1 ± 0.5 | — | 72.0 | — |
| `jReadStreet` | `size=512` | 207.9 ± 2.8 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 208.3 ± 0.6 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 215.6 ± 6.4 | — | 144.0 | — |
| `jReplaceId` | `size=512` | 2,749.3 ± 14.8 | — | 41,896.8 | — |
| `jReplaceId` | `size=64` | 331.2 ± 7.3 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 99.0 ± 4.2 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 88,995.8 ± 742.2 | — | 63,664.3 | — |
| `jSumPrices` | `size=64` | 10,895.6 ± 47.6 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,450.4 ± 10.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 66.4 ± 1.1 | — | 312.0 | — |
| `MapDrillModify` | `-` | 44.1 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 52.1 ± 1.2 | — | 200.0 | — |
| `handEnvUse` | `-` | 63.3 ± 3.0 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 38.7 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 49.0 ± 0.2 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.0 | 4.4 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 37.8 ± 0.4 | 31.2 ± 0.4 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.0 | 3.4 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 17,480.2 ± 1,417.8 | — | 43,036.5 | — |
| `Fold_powerEach` | `size=256` | 3,698.3 ± 101.2 | — | 9,240.3 | — |
| `Fold_powerEach` | `size=32` | 418.2 ± 5.8 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 79.8 ± 0.1 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 50,376.7 ± 287.0 | — | 331,119.8 | — |
| `Modify_multiFocus` | `size=256` | 12,010.1 ± 57.5 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,449.3 ± 19.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 215.8 ± 1.7 | — | 1,336.0 | — |
| `Modify_powerEach` | `size=1024` | 34,468.7 ± 4,318.9 | — | 115,176.9 | — |
| `Modify_powerEach` | `size=256` | 8,895.7 ± 95.2 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,077.9 ± 12.8 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 191.7 ± 0.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,596.3 ± 34.1 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,141.1 ± 8.6 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 245.9 ± 0.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 34.4 ± 0.1 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 5,090.4 ± 86.5 | — | 16,129.3 | — |
| `naive_sumQty` | `size=256` | 976.4 ± 47.0 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 75.1 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.5 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.7 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 193.0 ± 1.5 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 17.0 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 27.1 ± 0.0 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 40.4 ± 0.0 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.0 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.4 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 167.4 ± 0.9 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.8 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,036.6 ± 24.2 | — | 2,816.0 | — |
| `reuseUse` | `-` | 1,002.1 ± 13.8 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.1 ± 0.1 | 23.1 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 62.7 ± 0.2 | 69.8 ± 0.7 | 160.0 | 304.0 |
| `Modify_6` | `-` | 150.9 ± 1.6 | 116.1 ± 1.0 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 17.6 ± 0.3 | 17.5 ± 0.2 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.4 ± 0.0 | 1.5 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.7 ± 0.1 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.8 ± 0.0 | 7.4 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 36,227.9 ± 565.4 | — | 97,419.3 | — |
| `ModifyNames` | `size=64` | 4,579.4 ± 57.4 | — | 13,605.5 | — |
| `ModifyNames` | `size=8` | 608.9 ± 0.7 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 130.0 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 130.9 ± 1.1 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 130.8 ± 0.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 40.2 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.4 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.0 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,125.9 ± 768.0 | — | 382,773.6 | — |
| `monocleModifyNames` | `size=64` | 10,416.2 ± 528.9 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,424.5 ± 38.4 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 58,038.8 ± 220.8 | — | 169,084.5 | — |
| `monocleModifyStreet` | `size=64` | 7,220.5 ± 504.9 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 969.0 ± 5.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,475.7 ± 310.6 | — | 69,792.6 | — |
| `monocleReadStreet` | `size=64` | 4,712.7 ± 37.0 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 541.9 ± 1.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 72,244.5 ± 571.3 | — | 226,302.3 | — |
| `naiveModifyNames` | `size=64` | 8,584.6 ± 587.0 | — | 27,920.3 | — |
| `naiveModifyNames` | `size=8` | 1,133.4 ± 15.9 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 57,986.9 ± 241.0 | — | 169,063.3 | — |
| `naiveModifyStreet` | `size=64` | 7,558.8 ± 72.7 | — | 20,880.3 | — |
| `naiveModifyStreet` | `size=8` | 965.5 ± 4.5 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,454.0 ± 309.4 | — | 69,792.0 | — |
| `naiveReadStreet` | `size=64` | 4,706.8 ± 35.5 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 544.2 ± 6.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 231,002.2 ± 4,020.3 | — | 609,921.2 | — |
| `Names` | `size=64` | 29,120.1 ± 533.4 | — | 78,779.1 | — |
| `Names` | `size=8` | 4,018.2 ± 97.6 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 249,411.7 ± 3,250.4 | — | 683,543.9 | — |
| `NamesIor` | `size=64` | 31,856.9 ± 1,065.8 | — | 87,532.4 | — |
| `NamesIor` | `size=8` | 4,164.4 ± 49.5 | — | 11,520.1 | — |
| `Street` | `size=512` | 1,004.5 ± 14.2 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,017.9 ± 10.2 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,007.1 ± 16.2 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 999.5 ± 15.6 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 984.8 ± 5.5 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 992.3 ± 11.7 | — | 2,736.0 | — |
| `directNames` | `size=512` | 237,519.7 ± 3,789.9 | — | 613,990.4 | — |
| `directNames` | `size=64` | 29,266.8 ± 2,092.7 | — | 77,197.4 | — |
| `directNames` | `size=8` | 3,964.3 ± 59.3 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,002.8 ± 12.2 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,011.3 ± 11.2 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,012.1 ± 8.8 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 240,933.3 ± 9,031.7 | — | 609,889.1 | — |
| `hcursorNames` | `size=64` | 28,537.1 ± 613.8 | — | 77,265.6 | — |
| `hcursorNames` | `size=8` | 3,917.6 ± 25.7 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,043.4 ± 8.1 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,053.9 ± 23.4 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,057.6 ± 8.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 233,631.7 ± 9,241.6 | — | 1,121,770.3 | — |
| `monocleNames` | `size=64` | 24,733.7 ± 106.6 | — | 132,747.3 | — |
| `monocleNames` | `size=8` | 3,719.1 ± 78.0 | — | 19,509.4 | — |
| `monocleStreet` | `size=512` | 181,477.0 ± 6,817.2 | — | 908,033.9 | — |
| `monocleStreet` | `size=64` | 21,422.3 ± 96.1 | — | 113,804.6 | — |
| `monocleStreet` | `size=8` | 3,134.9 ± 50.0 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 195,192.9 ± 4,331.6 | — | 965,267.1 | — |
| `naiveNames` | `size=64` | 23,213.6 ± 70.4 | — | 120,842.1 | — |
| `naiveNames` | `size=8` | 3,273.0 ± 7.9 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 181,151.4 ± 5,953.7 | — | 908,025.7 | — |
| `naiveStreet` | `size=64` | 21,453.2 ± 60.6 | — | 113,801.9 | — |
| `naiveStreet` | `size=8` | 3,106.8 ± 13.8 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,098.8 ± 36.3 | — | 42,002.8 | — |
| `ModifyStreet` | `size=64` | 528.2 ± 2.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 303.1 ± 2.2 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 208.5 ± 3.0 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 211.7 ± 2.5 | — | 90.7 | — |
| `ReadStreet` | `size=8` | 209.5 ± 3.9 | — | 109.3 | — |
| `SumPrices` | `size=512` | 88,851.8 ± 709.4 | — | 63,716.3 | — |
| `SumPrices` | `size=64` | 10,943.7 ± 58.5 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,459.8 ± 7.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 168,133.9 ± 774.2 | — | 333,545.1 | — |
| `monocleModifyStreet` | `size=64` | 21,285.3 ± 560.8 | — | 30,082.1 | — |
| `monocleModifyStreet` | `size=8` | 3,404.6 ± 34.4 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 96,597.4 ± 270.8 | — | 193,234.7 | — |
| `monocleReadStreet` | `size=64` | 13,033.7 ± 1,335.7 | — | 24,705.3 | — |
| `monocleReadStreet` | `size=8` | 1,885.6 ± 26.8 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 450,557.9 ± 4,180.7 | — | 1,190,770.5 | — |
| `monocleSumPrices` | `size=64` | 16,774.7 ± 24.4 | — | 47,393.7 | — |
| `monocleSumPrices` | `size=8` | 2,550.5 ± 38.8 | — | 6,632.1 | — |
| `naiveModifyStreet` | `size=512` | 169,228.7 ± 2,176.2 | — | 333,504.0 | — |
| `naiveModifyStreet` | `size=64` | 20,901.0 ± 182.9 | — | 30,058.1 | — |
| `naiveModifyStreet` | `size=8` | 3,430.7 ± 64.6 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 97,156.6 ± 375.5 | — | 193,235.2 | — |
| `naiveReadStreet` | `size=64` | 12,323.0 ± 95.9 | — | 24,705.3 | — |
| `naiveReadStreet` | `size=8` | 1,930.1 ± 93.3 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 102,210.6 ± 764.1 | — | 230,127.5 | — |
| `naiveSumPrices` | `size=64` | 12,786.9 ± 54.0 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,948.3 ± 10.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 37,463.3 ± 533.5 | — | 455.4 | — |
| `nativeReadStreet` | `size=64` | 4,748.4 ± 29.6 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 865.9 ± 21.6 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 67,490.4 ± 275.0 | — | 86,275.0 | — |
| `nativeSumPrices` | `size=64` | 8,337.3 ± 32.4 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,250.1 ± 16.1 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 125,190.5 ± 302.2 | — | 624,387.1 | — |
| `TransformDeep` | `n=512` | 12,232.9 ± 128.9 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,425.6 ± 4.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 133,733.8 ± 1,455.2 | 178,108.0 ± 633.4 | 655,361.3 | 753,745.6 |
| `TransformExpr` | `n=512` | 16,478.6 ± 47.3 | 16,156.5 ± 133.3 | 81,825.7 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,026.6 ± 11.5 | 2,722.7 ± 4.2 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 111,670.1 ± 4,777.4 | — | 786,585.3 | — |
| `UniverseDeep` | `n=512` | 16,238.7 ± 384.5 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,977.8 ± 38.1 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 103,034.7 ± 4,951.8 | 2,847,681.5 ± 208,254.6 | 786,387.0 | 4,687,748.8 |
| `UniverseExpr` | `n=512` | 15,594.3 ± 64.4 | 178,939.3 ± 7,986.5 | 98,185.6 | 475,010.4 |
| `UniverseExpr` | `n=64` | 1,920.2 ± 32.0 | 14,723.7 ± 103.1 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 240,293.0 ± 2,953.4 | 3,122,211.4 ± 166,485.8 | 786,486.9 | 6,489,812.6 |
| `UniverseJson` | `n=512` | 27,507.6 ± 158.7 | 196,459.1 ± 6,967.6 | 98,186.8 | 699,916.2 |
| `UniverseJson` | `n=64` | 3,307.3 ± 47.8 | 18,448.8 ± 160.7 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 44,639.9 ± 655.2 | — | 163,888.5 | — |
| `visitorTransformDeep` | `n=512` | 4,089.3 ± 87.8 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 470.1 ± 4.1 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 69,325.2 ± 1,753.0 | — | 360,474.5 | — |
| `visitorTransformExpr` | `n=512` | 8,414.9 ± 18.5 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,068.4 ± 18.4 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,312.3 ± 462.5 | — | 196,707.2 | — |
| `visitorUniverseDeep` | `n=512` | 7,271.3 ± 47.5 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 825.5 ± 10.1 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,085.6 ± 198.2 | — | 196,656.1 | — |
| `visitorUniverseExpr` | `n=512` | 6,905.2 ± 19.4 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 826.3 ± 1.3 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 175,029.7 ± 29,105.4 | — | 461,991.4 | — |
| `visitorUniverseJson` | `n=512` | 21,908.0 ± 264.2 | — | 40,954.2 | — |
| `visitorUniverseJson` | `n=64` | 2,086.0 ± 51.0 | — | 4,088.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,477.5 ± 162.4 | — | 41,414.4 | — |
| `Modify_powerEach` | `size=16` | 270.2 ± 2.3 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,278.5 ± 48.0 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 118.9 ± 0.2 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 53,922.5 ± 931.3 | — | 164,375.8 | — |
| `Modify_powerEach` | `size=64` | 848.2 ± 6.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 57,005.3 ± 1,270.0 | — | 279,430.8 | — |
| `monocle_powerEach` | `size=16` | 582.3 ± 12.4 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,229.2 ± 170.5 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 236.6 ± 3.2 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 181,806.1 ± 1,080.1 | — | 967,848.0 | — |
| `monocle_powerEach` | `size=64` | 2,116.9 ± 6.4 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,390.1 ± 11.5 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 108.0 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,683.1 ± 11.7 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 27.8 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 21,526.8 ± 71.9 | — | 114,779.3 | — |
| `naive_powerEach` | `size=64` | 421.3 ± 0.6 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 73,365.5 ± 1,525.0 | — | 210,661.5 | — |
| `Modify_nested` | `size=16` | 1,449.8 ± 7.9 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 18,709.4 ± 39.9 | — | 53,771.7 | — |
| `Modify_nested` | `size=4` | 653.1 ± 9.8 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,835.6 ± 143.8 | — | 14,616.6 | — |
| `monocle_nested` | `size=1024` | 235,858.7 ± 1,453.5 | — | 1,118,829.4 | — |
| `monocle_nested` | `size=16` | 2,511.0 ± 129.9 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 89,716.5 ± 200.1 | — | 430,209.3 | — |
| `monocle_nested` | `size=4` | 1,232.2 ± 137.9 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,733.2 ± 108.1 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 20,745.0 ± 432.3 | — | 115,069.8 | — |
| `naive_nested` | `size=16` | 395.5 ± 10.6 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,498.7 ± 40.2 | — | 29,019.5 | — |
| `naive_nested` | `size=4` | 140.3 ± 4.3 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,378.5 ± 17.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,436.1 ± 1.5 | — | 4,840.1 | — |
| `Modify_sparse` | `size=2048` | 23,928.6 ± 459.8 | — | 104,700.0 | — |
| `Modify_sparse` | `size=32` | 373.8 ± 6.5 | — | 1,381.3 | — |
| `Modify_sparse` | `size=512` | 5,995.2 ± 11.7 | — | 24,809.6 | — |
| `Modify_sparse` | `size=8` | 125.2 ± 0.2 | — | 520.0 | — |
| `monocle_sparse` | `size=128` | 3,876.4 ± 45.9 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 97,345.4 ± 1,902.4 | — | 523,143.0 | — |
| `monocle_sparse` | `size=32` | 984.3 ± 2.8 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 31,819.1 ± 360.2 | — | 166,683.4 | — |
| `monocle_sparse` | `size=8` | 285.3 ± 2.2 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 335.7 ± 0.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,381.2 ± 20.3 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 86.4 ± 0.4 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,275.9 ± 11.1 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.9 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.2 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.4 ± 0.0 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.7 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.2 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 99,093.6 ± 1,108.0 | — | 589,712.7 | — |
| `Cata` | `-` | 76,974.7 ± 965.5 | — | 197,568.5 | — |
| `Hylo` | `-` | 86,151.9 ± 976.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 45,531.9 ± 309.1 | — | 327,632.3 | — |
| `drosteCata` | `-` | 41,277.1 ± 411.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 51,778.0 ± 96.5 | — | 328,640.4 | — |
| `handAna` | `-` | 20,605.8 ± 524.9 | — | 163,816.1 | — |
| `handCata` | `-` | 14,002.3 ± 28.5 | — | 0.1 | — |
| `handHylo` | `-` | 10,193.7 ± 740.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.8 ± 0.1 | 28.5 ± 0.4 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.2 ± 0.2 | 58.7 ± 0.1 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,395.0 ± 160.0 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 570.4 ± 2.4 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 57.2 ± 3.8 | — | 392.0 | — |
| `FoldPrices` | `size=512` | 3,047.2 ± 31.4 | 30,533.8 ± 1,160.1 | 12,312.5 | 162,580.8 |
| `FoldPrices` | `size=64` | 369.9 ± 0.4 | 2,161.5 ± 60.8 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 48.0 ± 0.4 | 285.6 ± 1.2 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 8,167.5 ± 231.2 | 32,739.3 ± 647.1 | 36,897.3 | 176,925.0 |
| `Modify` | `size=64` | 898.1 ± 0.6 | 1,788.7 ± 2.2 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 103.7 ± 2.0 | 241.2 ± 2.2 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 12.9 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 119.5 ± 0.3 | — | 528.0 | — |
| `ServiceGet` | `-` | 5.6 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 83.1 ± 0.2 | — | 456.0 | — |
| `handDrillGet` | `-` | 16.0 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 105.8 ± 0.2 | — | 648.0 | — |
| `handServiceGet` | `-` | 15.9 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 90.8 ± 6.7 | — | 576.0 | — |

