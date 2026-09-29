# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `bba8d79318c6e8f9fb580cb597f667008286864b` · date: `2026-09-29` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.5 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.4 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.5 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 7.9 ± 0.2 | 5.1 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 17.7 ± 1.2 | 13.4 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 77.8 ± 1.0 | — | 720.0 | — |
| `ModifyCountry` | `-` | 190.2 ± 11.2 | — | 968.0 | — |
| `ModifyPartner` | `-` | 225.7 ± 9.3 | — | 1,029.3 | — |
| `ReadCountry` | `-` | 109.7 ± 6.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 123.0 ± 3.3 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 191.4 ± 9.3 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 1,575.8 ± 35.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 1,783.7 ± 38.7 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,003.7 ± 26.6 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 961.7 ± 28.3 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,188.6 ± 37.2 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 494.6 ± 6.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 342.7 ± 4.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 215.2 ± 7.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 247.1 ± 16.5 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 782.0 ± 10.9 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 705.6 ± 21.0 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 30.5 ± 0.3 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 341.7 ± 2.5 | — | 568.0 | — |
| `handwritten_stream` | `-` | 104.3 ± 1.6 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 406.3 ± 17.0 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 1,933.6 ± 33.9 | — | 4,653.3 | — |
| `ClickToJson` | `-` | 1,727.7 ± 32.2 | — | 3,968.0 | — |
| `WideToAvro` | `-` | 465.9 ± 36.3 | — | 2,146.7 | — |
| `WideToJson` | `-` | 314.2 ± 12.2 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 988.9 ± 44.1 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 1,865.2 ± 22.3 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 642.4 ± 9.5 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,114.9 ± 22.7 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 122.1 ± 0.8 | — | 880.0 | — |
| `decode_native` | `-` | 10.7 ± 0.4 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 116.9 ± 2.8 | — | 880.0 | — |
| `encode_bridged` | `-` | 135.8 ± 2.8 | — | 1,234.7 | — |
| `encode_native` | `-` | 8.4 ± 0.7 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 133.4 ± 3.0 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 62.6 ± 3.2 | — | 432.0 | — |
| `fieldGet_native` | `-` | 62.1 ± 1.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 243.4 ± 46.0 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 92.7 ± 5.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 12.8 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 13.7 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 12.5 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 12.4 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 2.3 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 18.2 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 20.3 ± 1.7 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 2.8 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 2.5 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 2.1 ± 0.1 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 268.4 ± 7.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 273.0 ± 10.8 | — | 744.0 | — |
| `encode_hand` | `-` | 81.7 ± 1.5 | — | 768.0 | — |
| `encode_positional` | `-` | 3,186.0 ± 81.6 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 3,958.3 ± 124.1 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 2.8 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 8.3 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 15.6 ± 0.8 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 8.6 ± 0.5 | — | 184.0 | — |
| `reuseLeaf` | `-` | 1.6 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 9.4 ± 0.3 | — | 40.0 | — |
| `reuseLens3` | `-` | 25.4 ± 0.9 | — | 72.0 | — |
| `reuseLens6` | `-` | 71.4 ± 1.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 32.4 ± 0.7 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 2,231.0 ± 206.4 | 2,333.6 ± 208.5 | 14,080.4 | 14,080.4 |
| `FoldMap` | `size=64` | 198.9 ± 1.4 | 205.5 ± 10.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 12.5 ± 0.2 | 12.4 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,810.9 ± 38.6 | 1,767.7 ± 46.4 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 182.2 ± 9.4 | 178.2 ± 2.8 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 21.0 ± 0.1 | 21.2 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.0 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 1.7 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 1.5 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 1.6 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.3 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 1.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 8.6 ± 0.3 | 4.0 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 17.6 ± 0.1 | 12.3 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.9 ± 0.0 | 2.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 1.5 ± 0.0 | 1.6 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 224,190.6 ± 4,286.2 | — | 1,066,663.8 | — |
| `cModifyId` | `size=64` | 29,159.4 ± 721.2 | — | 136,243.6 | — |
| `cModifyId` | `size=8` | 4,773.4 ± 132.3 | — | 20,712.0 | — |
| `cReadId` | `size=512` | 106,492.1 ± 2,031.0 | — | 797,913.9 | — |
| `cReadId` | `size=64` | 13,965.6 ± 233.7 | — | 101,288.4 | — |
| `cReadId` | `size=8` | 2,163.3 ± 79.1 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 108,286.2 ± 4,029.5 | — | 797,912.8 | — |
| `cReadStreet` | `size=64` | 13,809.8 ± 257.1 | — | 101,288.4 | — |
| `cReadStreet` | `size=8` | 2,179.6 ± 46.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 224,918.0 ± 12,680.3 | — | 1,066,616.0 | — |
| `cReplaceId` | `size=64` | 29,023.4 ± 733.1 | — | 136,195.7 | — |
| `cReplaceId` | `size=8` | 4,817.6 ± 116.8 | — | 20,664.0 | — |
| `cSumPrices` | `size=512` | 187,302.4 ± 3,097.1 | — | 1,240,693.3 | — |
| `cSumPrices` | `size=64` | 23,400.0 ± 607.4 | — | 157,749.4 | — |
| `cSumPrices` | `size=8` | 3,249.0 ± 21.8 | — | 22,682.7 | — |
| `jMiss` | `size=512` | 100.4 ± 0.6 | — | 0.0 | — |
| `jMiss` | `size=64` | 98.2 ± 1.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 97.6 ± 1.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 1,814.1 ± 33.6 | — | 41,920.5 | — |
| `jModifyId` | `size=64` | 244.3 ± 4.3 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 62.0 ± 1.6 | — | 992.0 | — |
| `jReadId` | `size=512` | 20.5 ± 1.1 | — | 56.0 | — |
| `jReadId` | `size=64` | 25.7 ± 1.6 | — | 56.0 | — |
| `jReadId` | `size=8` | 22.9 ± 2.9 | — | 64.0 | — |
| `jReadStreet` | `size=512` | 112.3 ± 1.1 | — | 144.0 | — |
| `jReadStreet` | `size=64` | 114.3 ± 2.0 | — | 144.0 | — |
| `jReadStreet` | `size=8` | 113.9 ± 4.8 | — | 152.0 | — |
| `jReplaceId` | `size=512` | 1,779.7 ± 74.2 | — | 41,888.5 | — |
| `jReplaceId` | `size=64` | 248.9 ± 13.2 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 61.6 ± 0.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 46,491.0 ± 1,636.1 | — | 63,683.9 | — |
| `jSumPrices` | `size=64` | 5,982.2 ± 180.2 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 782.1 ± 38.2 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 36.7 ± 5.2 | — | 312.0 | — |
| `MapDrillModify` | `-` | 21.7 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 34.7 ± 0.6 | — | 200.0 | — |
| `handEnvUse` | `-` | 30.6 ± 0.6 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 19.8 ± 0.6 | — | 216.0 | — |
| `handMapGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 30.0 ± 5.2 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.5 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 2.1 ± 0.1 | 2.2 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 20.4 ± 0.7 | 18.4 ± 1.0 | 152.0 | 176.0 |
| `Replace` | `-` | 1.6 ± 0.0 | 1.7 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 10,338.8 ± 1,329.6 | — | 43,034.7 | — |
| `Fold_powerEach` | `size=256` | 2,083.4 ± 52.3 | — | 9,240.1 | — |
| `Fold_powerEach` | `size=32` | 230.6 ± 12.2 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 37.7 ± 0.7 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 32,273.3 ± 930.6 | — | 331,096.1 | — |
| `Modify_multiFocus` | `size=256` | 7,234.8 ± 109.4 | — | 76,112.5 | — |
| `Modify_multiFocus` | `size=32` | 927.8 ± 47.1 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 113.7 ± 2.5 | — | 1,336.0 | — |
| `Modify_powerEach` | `size=1024` | 21,080.5 ± 542.5 | — | 115,173.5 | — |
| `Modify_powerEach` | `size=256` | 5,022.9 ± 89.9 | — | 26,080.3 | — |
| `Modify_powerEach` | `size=32` | 623.4 ± 20.8 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 94.8 ± 2.2 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 5,472.1 ± 317.1 | — | 65,577.4 | — |
| `naive_listMap` | `size=256` | 1,304.8 ± 48.0 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 129.9 ± 6.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 16.6 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 2,958.7 ± 88.2 | — | 16,128.8 | — |
| `naive_sumQty` | `size=256` | 360.9 ± 8.3 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 40.1 ± 1.0 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 4.5 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 34.7 ± 0.9 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 93.6 ± 2.1 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.7 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 13.4 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 22.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 3.7 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 7.0 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 86.9 ± 1.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 25.2 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 547.4 ± 12.3 | — | 2,864.0 | — |
| `reuseUse` | `-` | 512.1 ± 12.7 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 12.7 ± 0.2 | 13.1 ± 0.2 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.6 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 31.9 ± 0.6 | 34.6 ± 0.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 78.6 ± 0.9 | 57.6 ± 1.3 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 8.6 ± 0.1 | 8.8 ± 0.3 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 2.3 ± 0.0 | 1.6 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 3.9 ± 0.2 | 3.3 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 21,016.0 ± 599.2 | — | 97,405.0 | — |
| `ModifyNames` | `size=64` | 2,548.4 ± 42.6 | — | 12,605.4 | — |
| `ModifyNames` | `size=8` | 349.6 ± 6.0 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 74.7 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 74.6 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 76.9 ± 1.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 22.9 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 23.0 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 22.9 ± 0.4 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 59,262.2 ± 565.7 | — | 382,781.5 | — |
| `monocleModifyNames` | `size=64` | 5,904.3 ± 88.8 | — | 39,856.2 | — |
| `monocleModifyNames` | `size=8` | 837.0 ± 39.2 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 31,177.7 ± 120.8 | — | 169,067.2 | — |
| `monocleModifyStreet` | `size=64` | 3,782.6 ± 286.1 | — | 20,896.1 | — |
| `monocleModifyStreet` | `size=8` | 505.1 ± 10.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 19,012.3 ± 287.6 | — | 69,780.6 | — |
| `monocleReadStreet` | `size=64` | 2,439.3 ± 88.5 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 275.4 ± 1.4 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 40,906.1 ± 823.0 | — | 226,284.3 | — |
| `naiveModifyNames` | `size=64` | 4,910.8 ± 23.9 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 660.9 ± 7.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 31,500.5 ± 385.2 | — | 169,043.5 | — |
| `naiveModifyStreet` | `size=64` | 3,959.6 ± 24.5 | — | 20,880.1 | — |
| `naiveModifyStreet` | `size=8` | 516.1 ± 11.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 18,959.2 ± 276.7 | — | 69,780.5 | — |
| `naiveReadStreet` | `size=64` | 2,397.7 ± 11.9 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 275.9 ± 1.9 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 116,359.2 ± 2,358.8 | — | 613,933.3 | — |
| `Names` | `size=64` | 14,545.1 ± 324.1 | — | 78,257.5 | — |
| `Names` | `size=8` | 2,056.1 ± 46.7 | — | 10,944.0 | — |
| `NamesIor` | `size=512` | 133,273.1 ± 7,499.9 | — | 683,450.8 | — |
| `NamesIor` | `size=64` | 16,694.0 ± 466.9 | — | 87,009.8 | — |
| `NamesIor` | `size=8` | 2,183.4 ± 132.1 | — | 11,592.0 | — |
| `Street` | `size=512` | 538.3 ± 18.3 | — | 2,720.4 | — |
| `Street` | `size=64` | 523.7 ± 10.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 533.0 ± 9.5 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 534.1 ± 22.8 | — | 2,736.4 | — |
| `StreetIor` | `size=64` | 530.9 ± 8.7 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 541.5 ± 17.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 117,332.9 ± 2,826.5 | — | 609,799.7 | — |
| `directNames` | `size=64` | 14,725.9 ± 92.1 | — | 77,713.6 | — |
| `directNames` | `size=8` | 1,961.6 ± 19.9 | — | 10,616.0 | — |
| `directStreet` | `size=512` | 525.4 ± 13.9 | — | 2,736.4 | — |
| `directStreet` | `size=64` | 523.7 ± 3.5 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 526.3 ± 10.3 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 117,549.5 ± 4,032.5 | — | 609,809.8 | — |
| `hcursorNames` | `size=64` | 14,728.6 ± 504.5 | — | 76,737.6 | — |
| `hcursorNames` | `size=8` | 1,995.0 ± 87.2 | — | 10,696.0 | — |
| `hcursorStreet` | `size=512` | 577.1 ± 16.9 | — | 3,032.5 | — |
| `hcursorStreet` | `size=64` | 566.9 ± 16.7 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 560.3 ± 12.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 123,653.9 ± 3,663.2 | — | 1,121,691.9 | — |
| `monocleNames` | `size=64` | 14,634.3 ± 405.6 | — | 132,750.6 | — |
| `monocleNames` | `size=8` | 2,173.1 ± 109.2 | — | 19,477.4 | — |
| `monocleStreet` | `size=512` | 94,012.4 ± 2,229.9 | — | 907,983.2 | — |
| `monocleStreet` | `size=64` | 12,177.4 ± 287.7 | — | 113,798.4 | — |
| `monocleStreet` | `size=8` | 1,711.2 ± 8.5 | — | 17,048.0 | — |
| `naiveNames` | `size=512` | 103,610.6 ± 2,191.5 | — | 965,214.7 | — |
| `naiveNames` | `size=64` | 13,311.8 ± 312.1 | — | 120,837.4 | — |
| `naiveNames` | `size=8` | 1,839.8 ± 41.9 | — | 17,808.0 | — |
| `naiveStreet` | `size=512` | 95,038.8 ± 2,151.3 | — | 907,994.1 | — |
| `naiveStreet` | `size=64` | 12,415.2 ± 224.3 | — | 113,811.8 | — |
| `naiveStreet` | `size=8` | 1,787.2 ± 45.7 | — | 17,040.0 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 1,928.3 ± 86.3 | — | 42,025.7 | — |
| `ModifyStreet` | `size=64` | 374.6 ± 14.9 | — | 5,440.0 | — |
| `ModifyStreet` | `size=8` | 168.9 ± 4.2 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 115.9 ± 5.1 | — | 109.4 | — |
| `ReadStreet` | `size=64` | 115.8 ± 1.1 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 111.1 ± 2.6 | — | 90.7 | — |
| `SumPrices` | `size=512` | 45,667.0 ± 1,885.4 | — | 63,711.8 | — |
| `SumPrices` | `size=64` | 5,868.7 ± 127.0 | — | 8,120.7 | — |
| `SumPrices` | `size=8` | 778.5 ± 27.3 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 93,246.8 ± 2,552.6 | — | 333,504.3 | — |
| `monocleModifyStreet` | `size=64` | 11,453.8 ± 169.6 | — | 30,081.2 | — |
| `monocleModifyStreet` | `size=8` | 2,046.6 ± 78.5 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 52,648.3 ± 992.7 | — | 193,197.1 | — |
| `monocleReadStreet` | `size=64` | 6,662.3 ± 186.4 | — | 24,704.7 | — |
| `monocleReadStreet` | `size=8` | 1,042.8 ± 25.7 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 226,218.1 ± 10,318.0 | — | 1,190,577.6 | — |
| `monocleSumPrices` | `size=64` | 9,457.2 ± 213.9 | — | 47,393.0 | — |
| `monocleSumPrices` | `size=8` | 1,559.1 ± 29.4 | — | 6,640.0 | — |
| `naiveModifyStreet` | `size=512` | 92,827.0 ± 672.7 | — | 333,509.6 | — |
| `naiveModifyStreet` | `size=64` | 11,538.0 ± 133.7 | — | 30,057.2 | — |
| `naiveModifyStreet` | `size=8` | 2,014.5 ± 28.7 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 52,637.4 ± 422.5 | — | 193,197.1 | — |
| `naiveReadStreet` | `size=64` | 6,644.0 ± 91.2 | — | 24,704.7 | — |
| `naiveReadStreet` | `size=8` | 1,031.4 ± 4.3 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 57,222.8 ± 1,062.1 | — | 230,089.0 | — |
| `naiveSumPrices` | `size=64` | 7,386.6 ± 132.5 | — | 29,336.8 | — |
| `naiveSumPrices` | `size=8` | 1,080.7 ± 12.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 21,443.1 ± 337.3 | — | 441.9 | — |
| `nativeReadStreet` | `size=64` | 2,771.2 ± 37.6 | — | 424.3 | — |
| `nativeReadStreet` | `size=8` | 501.2 ± 10.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 38,454.8 ± 746.9 | — | 86,263.3 | — |
| `nativeSumPrices` | `size=64` | 4,767.4 ± 77.5 | — | 10,920.5 | — |
| `nativeSumPrices` | `size=8` | 695.2 ± 15.7 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 73,296.3 ± 1,828.2 | — | 624,349.3 | — |
| `TransformDeep` | `n=512` | 9,129.8 ± 566.7 | — | 57,360.9 | — |
| `TransformDeep` | `n=64` | 1,080.1 ± 28.3 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 73,323.2 ± 1,805.9 | 90,949.0 ± 764.1 | 655,317.4 | 753,682.2 |
| `TransformExpr` | `n=512` | 9,421.4 ± 238.7 | 8,387.0 ± 198.9 | 81,825.0 | 69,584.9 |
| `TransformExpr` | `n=64` | 1,156.9 ± 46.0 | 1,401.0 ± 28.8 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 66,154.8 ± 2,709.7 | — | 786,552.2 | — |
| `UniverseDeep` | `n=512` | 8,775.4 ± 171.8 | — | 98,376.9 | — |
| `UniverseDeep` | `n=64` | 1,078.9 ± 17.0 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 66,758.5 ± 3,256.0 | 2,628,910.7 ± 101,957.4 | 786,360.6 | 4,687,589.8 |
| `UniverseExpr` | `n=512` | 8,479.6 ± 193.4 | 208,786.1 ± 4,723.2 | 98,184.9 | 475,013.4 |
| `UniverseExpr` | `n=64` | 980.4 ± 14.0 | 18,529.0 ± 480.5 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 141,542.0 ± 3,445.7 | 2,823,278.8 ± 95,248.8 | 786,415.0 | 6,489,595.5 |
| `UniverseJson` | `n=512` | 16,432.6 ± 1,279.3 | 217,783.5 ± 1,792.4 | 98,185.7 | 699,918.5 |
| `UniverseJson` | `n=64` | 1,865.3 ± 43.6 | 20,545.3 ± 419.7 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 22,873.8 ± 1,398.4 | — | 163,872.6 | — |
| `visitorTransformDeep` | `n=512` | 2,492.3 ± 42.5 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 236.0 ± 3.9 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 38,871.0 ± 534.9 | — | 360,452.3 | — |
| `visitorTransformExpr` | `n=512` | 4,655.0 ± 49.4 | — | 45,032.5 | — |
| `visitorTransformExpr` | `n=64` | 587.1 ± 16.7 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 38,745.9 ± 2,345.5 | — | 196,692.2 | — |
| `visitorUniverseDeep` | `n=512` | 4,580.5 ± 123.2 | — | 24,632.5 | — |
| `visitorUniverseDeep` | `n=64` | 476.7 ± 20.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 32,660.1 ± 557.3 | — | 196,639.8 | — |
| `visitorUniverseExpr` | `n=512` | 3,747.6 ± 14.4 | — | 24,584.4 | — |
| `visitorUniverseExpr` | `n=64` | 450.8 ± 12.3 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 94,612.3 ± 16,194.8 | — | 385,476.9 | — |
| `visitorUniverseJson` | `n=512` | 11,786.3 ± 186.8 | — | 40,953.2 | — |
| `visitorUniverseJson` | `n=64` | 1,124.9 ± 22.2 | — | 4,760.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 7,865.2 ± 123.9 | — | 41,411.7 | — |
| `Modify_powerEach` | `size=16` | 140.9 ± 1.9 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 1,968.7 ± 38.5 | — | 10,688.3 | — |
| `Modify_powerEach` | `size=4` | 60.7 ± 1.4 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 29,765.0 ± 701.4 | — | 164,336.5 | — |
| `Modify_powerEach` | `size=64` | 505.2 ± 11.7 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 30,361.5 ± 562.7 | — | 279,408.2 | — |
| `monocle_powerEach` | `size=16` | 400.5 ± 38.6 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 12,265.4 ± 855.3 | — | 107,331.1 | — |
| `monocle_powerEach` | `size=4` | 126.5 ± 3.3 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 105,249.6 ± 1,694.2 | — | 967,729.5 | — |
| `monocle_powerEach` | `size=64` | 1,287.5 ± 47.4 | — | 14,520.0 | — |
| `naive_powerEach` | `size=1024` | 3,890.7 ± 60.2 | — | 28,729.8 | — |
| `naive_powerEach` | `size=16` | 48.8 ± 0.8 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 766.3 ± 11.0 | — | 7,224.1 | — |
| `naive_powerEach` | `size=4` | 14.9 ± 0.4 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 12,600.7 ± 218.9 | — | 114,764.6 | — |
| `naive_powerEach` | `size=64` | 177.9 ± 5.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 38,314.3 ± 3,102.6 | — | 210,597.8 | — |
| `Modify_nested` | `size=16` | 911.9 ± 19.1 | — | 4,792.0 | — |
| `Modify_nested` | `size=256` | 10,533.9 ± 300.9 | — | 53,726.7 | — |
| `Modify_nested` | `size=4` | 365.4 ± 4.0 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 2,794.0 ± 43.2 | — | 14,592.3 | — |
| `monocle_nested` | `size=1024` | 123,109.2 ± 1,227.8 | — | 1,118,625.2 | — |
| `monocle_nested` | `size=16` | 1,479.4 ± 58.7 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 49,860.6 ± 374.3 | — | 430,185.8 | — |
| `monocle_nested` | `size=4` | 768.2 ± 26.8 | — | 5,536.0 | — |
| `monocle_nested` | `size=64` | 4,852.3 ± 205.8 | — | 58,912.5 | — |
| `naive_nested` | `size=1024` | 13,006.7 ± 325.4 | — | 115,055.7 | — |
| `naive_nested` | `size=16` | 198.8 ± 5.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,451.6 ± 85.0 | — | 29,018.2 | — |
| `naive_nested` | `size=4` | 72.3 ± 0.8 | — | 792.0 | — |
| `naive_nested` | `size=64` | 801.0 ± 74.1 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 776.8 ± 22.1 | — | 4,816.0 | — |
| `Modify_sparse` | `size=2048` | 13,032.7 ± 396.3 | — | 104,666.4 | — |
| `Modify_sparse` | `size=32` | 185.9 ± 2.8 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 3,306.3 ± 22.1 | — | 24,784.9 | — |
| `Modify_sparse` | `size=8` | 60.8 ± 1.2 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 2,452.1 ± 31.5 | — | 27,808.1 | — |
| `monocle_sparse` | `size=2048` | 53,317.9 ± 1,826.6 | — | 523,111.5 | — |
| `monocle_sparse` | `size=32` | 772.4 ± 22.5 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 17,262.0 ± 203.5 | — | 166,676.2 | — |
| `monocle_sparse` | `size=8` | 183.2 ± 5.5 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 161.5 ± 1.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 3,179.4 ± 53.2 | — | 24,610.6 | — |
| `naive_sparse` | `size=32` | 41.9 ± 1.0 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 741.2 ± 15.9 | — | 6,176.2 | — |
| `naive_sparse` | `size=8` | 12.9 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.5 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.3 ± 0.0 | 1.5 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.2 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 10.5 ± 0.3 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 18.8 ± 0.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.0 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 2.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 4.5 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 51,592.9 ± 1,508.8 | — | 589,712.4 | — |
| `Cata` | `-` | 41,332.6 ± 327.4 | — | 197,568.3 | — |
| `Hylo` | `-` | 44,021.8 ± 1,153.6 | — | 295,848.3 | — |
| `drosteAna` | `-` | 23,745.5 ± 544.8 | — | 327,632.2 | — |
| `drosteCata` | `-` | 23,419.7 ± 682.3 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 26,758.6 ± 792.7 | — | 328,640.2 | — |
| `handAna` | `-` | 10,046.2 ± 189.3 | — | 163,816.1 | — |
| `handCata` | `-` | 6,955.9 ± 88.2 | — | 0.0 | — |
| `handHylo` | `-` | 5,958.0 ± 340.0 | — | 0.0 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 6.5 ± 0.1 | 14.3 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 14.7 ± 0.1 | 30.6 ± 1.0 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 1.6 ± 0.1 | 1.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 3,182.8 ± 119.2 | — | 20,200.5 | — |
| `FoldNested` | `size=64` | 312.4 ± 7.5 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 27.2 ± 0.6 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,701.6 ± 35.7 | 16,430.5 ± 541.8 | 12,312.3 | 162,578.6 |
| `FoldPrices` | `size=64` | 179.2 ± 2.8 | 1,393.8 ± 53.9 | 1,560.0 | 15,424.0 |
| `FoldPrices` | `size=8` | 21.5 ± 0.3 | 177.1 ± 1.3 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 5,128.6 ± 75.2 | 18,179.2 ± 401.6 | 36,896.8 | 176,922.9 |
| `Modify` | `size=64` | 706.7 ± 40.0 | 1,180.8 ± 27.0 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 50.8 ± 0.9 | 169.6 ± 4.7 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 6.4 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 75.3 ± 1.4 | — | 528.0 | — |
| `ServiceGet` | `-` | 2.7 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 57.6 ± 0.7 | — | 456.0 | — |
| `handDrillGet` | `-` | 8.0 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 67.9 ± 0.9 | — | 648.0 | — |
| `handServiceGet` | `-` | 8.2 ± 0.3 | — | 120.0 | — |
| `handServiceReplace` | `-` | 60.6 ± 1.6 | — | 576.0 | — |

