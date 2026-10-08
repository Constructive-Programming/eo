# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `241d627f9f303af3620ddb6881ffa9e37247d313` · date: `2026-10-08` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.1 ± 0.3 | 9.7 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 26.7 ± 0.2 | 23.7 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 159.7 ± 0.5 | — | 720.0 | — |
| `ModifyCountry` | `-` | 302.7 ± 8.1 | — | 928.0 | — |
| `ModifyPartner` | `-` | 392.1 ± 9.0 | — | 994.7 | — |
| `ReadCountry` | `-` | 173.7 ± 2.1 | — | 520.0 | — |
| `ReadPartner` | `-` | 208.4 ± 3.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 312.4 ± 7.0 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,634.4 ± 43.6 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,942.0 ± 53.5 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,866.5 ± 51.0 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,626.8 ± 27.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,021.1 ± 13.6 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 935.5 ± 10.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 613.6 ± 10.7 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 402.2 ± 1.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 483.3 ± 91.2 | — | 1,565.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,375.4 ± 10.2 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,341.3 ± 13.5 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.6 ± 0.3 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 633.6 ± 6.0 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.0 ± 1.2 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 657.4 ± 13.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,961.1 ± 14.4 | — | 4,632.0 | — |
| `ClickToJson` | `-` | 2,819.4 ± 15.8 | — | 3,978.7 | — |
| `WideToAvro` | `-` | 867.2 ± 11.1 | — | 2,040.0 | — |
| `WideToJson` | `-` | 682.5 ± 35.7 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,464.0 ± 5.8 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,940.0 ± 66.5 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 961.2 ± 15.8 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,875.4 ± 7.4 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 191.4 ± 1.3 | — | 832.0 | — |
| `decode_native` | `-` | 17.2 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 184.8 ± 1.2 | — | 832.0 | — |
| `encode_bridged` | `-` | 245.0 ± 0.7 | — | 1,224.0 | — |
| `encode_native` | `-` | 11.7 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 256.5 ± 3.0 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 99.0 ± 0.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.6 ± 1.7 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 398.2 ± 8.4 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 166.7 ± 5.5 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.1 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.6 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 32.9 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 534.0 ± 16.2 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 540.9 ± 13.4 | — | 744.0 | — |
| `encode_hand` | `-` | 140.2 ± 0.9 | — | 768.0 | — |
| `encode_positional` | `-` | 6,350.4 ± 49.0 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,795.9 ± 38.0 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.4 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 38.5 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.0 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.6 ± 0.3 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.4 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 123.6 ± 0.4 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 57.5 ± 0.3 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,232.5 ± 7.1 | 1,234.1 ± 7.5 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.8 ± 0.6 | 156.5 ± 0.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.0 | 21.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,154.5 ± 39.5 | 2,168.0 ± 29.1 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 154.7 ± 0.7 | 154.8 ± 0.6 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.6 ± 0.1 | 26.3 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.2 ± 0.0 | 9.4 ± 0.5 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.9 ± 0.6 | 24.6 ± 0.3 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 404,945.2 ± 5,449.7 | — | 1,066,714.5 | — |
| `cModifyId` | `size=64` | 51,904.3 ± 894.4 | — | 136,269.5 | — |
| `cModifyId` | `size=8` | 8,451.9 ± 127.4 | — | 20,696.1 | — |
| `cReadId` | `size=512` | 206,885.3 ± 1,469.7 | — | 797,930.8 | — |
| `cReadId` | `size=64` | 25,481.6 ± 220.7 | — | 101,293.6 | — |
| `cReadId` | `size=8` | 4,046.8 ± 147.5 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 215,020.3 ± 17,255.0 | — | 797,933.1 | — |
| `cReadStreet` | `size=64` | 26,311.7 ± 467.8 | — | 101,294.4 | — |
| `cReadStreet` | `size=8` | 3,971.4 ± 39.8 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 402,462.7 ± 6,223.5 | — | 1,066,674.5 | — |
| `cReplaceId` | `size=64` | 52,196.9 ± 532.8 | — | 136,221.6 | — |
| `cReplaceId` | `size=8` | 8,543.0 ± 137.1 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 349,297.6 ± 7,123.5 | — | 1,240,743.8 | — |
| `cSumPrices` | `size=64` | 42,232.9 ± 1,100.2 | — | 157,049.0 | — |
| `cSumPrices` | `size=8` | 6,159.7 ± 54.4 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 182.6 ± 2.0 | — | 0.1 | — |
| `jMiss` | `size=64` | 178.3 ± 8.3 | — | 0.0 | — |
| `jMiss` | `size=8` | 178.3 ± 8.3 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,816.7 ± 22.7 | — | 41,929.1 | — |
| `jModifyId` | `size=64` | 498.0 ± 10.0 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 110.3 ± 0.4 | — | 976.0 | — |
| `jReadId` | `size=512` | 34.7 ± 1.1 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.0 ± 0.9 | — | 48.0 | — |
| `jReadId` | `size=8` | 34.2 ± 0.9 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 199.0 ± 8.2 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 200.2 ± 8.7 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 194.6 ± 1.9 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,723.5 ± 22.1 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 465.6 ± 12.1 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 105.5 ± 0.8 | — | 936.0 | — |
| `jSumPrices` | `size=512` | 84,532.6 ± 977.8 | — | 63,662.2 | — |
| `jSumPrices` | `size=64` | 10,617.0 ± 378.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,447.1 ± 41.5 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 87.1 ± 2.5 | — | 320.0 | — |
| `MapDrillModify` | `-` | 38.8 ± 0.2 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 65.5 ± 1.8 | — | 216.0 | — |
| `handEnvUse` | `-` | 84.9 ± 0.5 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 33.8 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 61.9 ± 0.5 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 32.9 ± 0.2 | 29.6 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,372.3 ± 730.9 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,159.1 ± 70.2 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 374.5 ± 2.0 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 84.6 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,667.4 ± 1,957.9 | — | 331,150.9 | — |
| `Modify_multiFocus` | `size=256` | 11,156.4 ± 26.7 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,706.9 ± 524.3 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 214.1 ± 1.1 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,516.3 ± 462.8 | — | 115,176.9 | — |
| `Modify_powerEach` | `size=256` | 8,219.6 ± 60.5 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,015.3 ± 12.3 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 193.7 ± 5.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,301.3 ± 28.9 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,053.6 ± 7.2 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 250.0 ± 0.5 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.6 ± 0.1 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,136.6 ± 65.8 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 717.7 ± 27.2 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.1 ± 0.2 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.3 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.9 ± 2.3 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 122.3 ± 0.2 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.6 ± 0.0 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.6 ± 0.3 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.5 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.5 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.0 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 159.6 ± 2.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.4 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,125.5 ± 20.9 | — | 2,840.0 | — |
| `reuseUse` | `-` | 1,056.0 ± 11.6 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.2 ± 0.1 | 21.5 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.2 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 57.5 ± 0.2 | 71.5 ± 0.2 | 160.0 | 304.0 |
| `Modify_6` | `-` | 135.7 ± 0.6 | 119.9 ± 1.5 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.5 ± 0.0 | 16.1 ± 0.0 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.5 ± 0.0 | 7.0 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,058.8 ± 478.7 | — | 101,519.8 | — |
| `ModifyNames` | `size=64` | 4,044.2 ± 76.6 | — | 13,640.1 | — |
| `ModifyNames` | `size=8` | 562.6 ± 5.9 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 111.8 ± 0.9 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.7 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.6 ± 1.1 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.3 ± 1.7 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.0 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 92,552.9 ± 3,096.5 | — | 382,775.0 | — |
| `monocleModifyNames` | `size=64` | 9,006.0 ± 90.7 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,385.9 ± 5.5 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,771.8 ± 385.3 | — | 169,079.6 | — |
| `monocleModifyStreet` | `size=64` | 5,866.9 ± 176.7 | — | 20,888.2 | — |
| `monocleModifyStreet` | `size=8` | 922.3 ± 7.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 24,951.6 ± 101.5 | — | 69,783.3 | — |
| `monocleReadStreet` | `size=64` | 3,215.9 ± 13.9 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 471.9 ± 2.2 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,751.2 ± 359.3 | — | 226,296.2 | — |
| `naiveModifyNames` | `size=64` | 7,559.9 ± 50.5 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,080.7 ± 5.6 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,666.9 ± 298.0 | — | 169,056.7 | — |
| `naiveModifyStreet` | `size=64` | 5,982.1 ± 52.4 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 916.6 ± 17.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 24,993.4 ± 121.2 | — | 69,783.3 | — |
| `naiveReadStreet` | `size=64` | 3,213.6 ± 14.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 472.6 ± 2.1 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 245,551.8 ± 3,619.6 | — | 614,036.8 | — |
| `Names` | `size=64` | 30,996.8 ± 560.0 | — | 79,299.3 | — |
| `Names` | `size=8` | 4,339.2 ± 100.6 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 256,913.2 ± 2,600.2 | — | 675,349.9 | — |
| `NamesIor` | `size=64` | 33,445.8 ± 839.5 | — | 87,497.5 | — |
| `NamesIor` | `size=8` | 4,627.1 ± 35.6 | — | 11,504.1 | — |
| `Street` | `size=512` | 1,049.0 ± 5.6 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,056.8 ± 21.7 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,055.9 ± 21.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,056.2 ± 11.9 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,037.5 ± 24.0 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,094.1 ± 39.1 | — | 2,736.0 | — |
| `directNames` | `size=512` | 244,094.3 ± 10,728.2 | — | 613,995.7 | — |
| `directNames` | `size=64` | 31,078.6 ± 756.4 | — | 77,708.3 | — |
| `directNames` | `size=8` | 4,068.3 ± 88.7 | — | 10,616.1 | — |
| `directStreet` | `size=512` | 1,062.3 ± 10.2 | — | 2,752.8 | — |
| `directStreet` | `size=64` | 1,044.2 ± 20.8 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,031.7 ± 23.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 254,558.3 ± 3,703.6 | — | 614,004.0 | — |
| `hcursorNames` | `size=64` | 29,230.5 ± 429.2 | — | 77,790.8 | — |
| `hcursorNames` | `size=8` | 4,289.9 ± 124.3 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,136.6 ± 20.0 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,101.2 ± 9.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,135.4 ± 36.8 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 229,851.4 ± 5,918.7 | — | 1,121,762.4 | — |
| `monocleNames` | `size=64` | 23,705.1 ± 424.8 | — | 132,071.1 | — |
| `monocleNames` | `size=8` | 3,665.9 ± 17.8 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 179,959.8 ± 2,925.6 | — | 908,030.3 | — |
| `monocleStreet` | `size=64` | 20,993.3 ± 453.0 | — | 113,460.5 | — |
| `monocleStreet` | `size=8` | 3,198.3 ± 61.4 | — | 17,069.4 | — |
| `naiveNames` | `size=512` | 195,153.7 ± 4,002.8 | — | 965,267.1 | — |
| `naiveNames` | `size=64` | 22,727.0 ± 411.9 | — | 120,493.2 | — |
| `naiveNames` | `size=8` | 3,329.2 ± 25.2 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 182,327.5 ± 2,960.0 | — | 908,053.2 | — |
| `naiveStreet` | `size=64` | 21,112.2 ± 89.9 | — | 113,780.6 | — |
| `naiveStreet` | `size=8` | 3,152.7 ± 16.0 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,032.3 ± 27.3 | — | 42,003.6 | — |
| `ModifyStreet` | `size=64` | 663.6 ± 12.2 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 297.7 ± 9.7 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 192.7 ± 2.8 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 204.0 ± 8.5 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 197.0 ± 8.5 | — | 109.3 | — |
| `SumPrices` | `size=512` | 85,784.9 ± 630.7 | — | 63,715.8 | — |
| `SumPrices` | `size=64` | 10,528.7 ± 186.4 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,437.9 ± 9.8 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 157,150.8 ± 1,061.1 | — | 333,586.7 | — |
| `monocleModifyStreet` | `size=64` | 18,943.8 ± 80.3 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,169.9 ± 24.5 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 87,941.8 ± 408.2 | — | 193,227.3 | — |
| `monocleReadStreet` | `size=64` | 11,248.2 ± 167.4 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,770.0 ± 14.5 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 418,527.2 ± 3,731.4 | — | 1,178,440.0 | — |
| `monocleSumPrices` | `size=64` | 15,377.3 ± 340.2 | — | 45,825.6 | — |
| `monocleSumPrices` | `size=8` | 2,427.3 ± 35.3 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,361.9 ± 1,508.5 | — | 333,563.5 | — |
| `naiveModifyStreet` | `size=64` | 19,124.3 ± 448.2 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,153.0 ± 33.2 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 87,980.6 ± 512.1 | — | 193,227.3 | — |
| `naiveReadStreet` | `size=64` | 11,321.0 ± 172.3 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,783.3 ± 25.0 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,747.6 ± 692.2 | — | 230,119.4 | — |
| `naiveSumPrices` | `size=64` | 12,043.0 ± 36.9 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,870.1 ± 11.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,127.8 ± 408.1 | — | 451.5 | — |
| `nativeReadStreet` | `size=64` | 4,252.6 ± 75.2 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 807.9 ± 14.2 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 61,914.6 ± 311.8 | — | 86,271.1 | — |
| `nativeSumPrices` | `size=64` | 7,644.9 ± 24.1 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,153.9 ± 11.0 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 118,087.2 ± 316.1 | — | 624,381.9 | — |
| `TransformDeep` | `n=512` | 11,654.1 ± 40.5 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,413.0 ± 6.6 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 128,784.7 ± 414.7 | 162,315.7 ± 978.9 | 655,357.7 | 753,734.1 |
| `TransformExpr` | `n=512` | 15,855.5 ± 62.0 | 15,323.8 ± 232.1 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,986.1 ± 7.0 | 2,487.1 ± 22.4 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 100,808.5 ± 6,300.3 | — | 786,577.4 | — |
| `UniverseDeep` | `n=512` | 15,108.8 ± 57.2 | — | 98,377.5 | — |
| `UniverseDeep` | `n=64` | 1,856.6 ± 5.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 97,442.7 ± 1,548.2 | 2,827,567.6 ± 165,395.0 | 786,382.9 | 4,687,735.1 |
| `UniverseExpr` | `n=512` | 14,675.1 ± 32.1 | 200,021.2 ± 6,728.7 | 98,185.5 | 475,012.5 |
| `UniverseExpr` | `n=64` | 1,790.6 ± 5.9 | 17,883.9 ± 624.1 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 217,766.6 ± 2,200.6 | 3,118,125.8 ± 119,930.8 | 786,470.5 | 6,489,810.0 |
| `UniverseJson` | `n=512` | 24,652.2 ± 94.0 | 239,221.5 ± 7,928.0 | 98,186.5 | 699,920.7 |
| `UniverseJson` | `n=64` | 2,964.1 ± 7.7 | 22,746.7 ± 893.9 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,167.3 ± 203.0 | — | 163,886.7 | — |
| `visitorTransformDeep` | `n=512` | 4,880.8 ± 26.6 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 424.0 ± 1.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,019.7 ± 594.7 | — | 360,470.6 | — |
| `visitorTransformExpr` | `n=512` | 7,522.0 ± 23.7 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 977.1 ± 3.8 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,298.3 ± 533.8 | — | 196,703.5 | — |
| `visitorUniverseDeep` | `n=512` | 6,472.9 ± 56.2 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 743.1 ± 3.4 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,863.9 ± 478.4 | — | 196,654.5 | — |
| `visitorUniverseExpr` | `n=512` | 6,483.2 ± 15.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 795.6 ± 2.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 154,783.4 ± 10,628.8 | — | 371,464.7 | — |
| `visitorUniverseJson` | `n=512` | 22,948.3 ± 2,426.7 | — | 47,754.3 | — |
| `visitorUniverseJson` | `n=64` | 2,176.2 ± 150.6 | — | 4,760.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,120.0 ± 41.4 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 264.4 ± 8.4 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,961.6 ± 41.1 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.6 ± 0.9 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,690.0 ± 1,952.6 | — | 164,372.5 | — |
| `Modify_powerEach` | `size=64` | 772.4 ± 18.9 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 62,155.1 ± 1,256.2 | — | 279,434.4 | — |
| `monocle_powerEach` | `size=16` | 607.6 ± 16.1 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,621.2 ± 298.9 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 214.0 ± 9.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 181,513.1 ± 4,530.4 | — | 967,847.6 | — |
| `monocle_powerEach` | `size=64` | 2,123.3 ± 58.4 | — | 14,509.4 | — |
| `naive_powerEach` | `size=1024` | 6,704.0 ± 10.0 | — | 28,731.2 | — |
| `naive_powerEach` | `size=16` | 100.9 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,614.1 ± 6.1 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 24.9 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,842.5 ± 60.0 | — | 114,787.7 | — |
| `naive_powerEach` | `size=64` | 396.9 ± 2.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 72,694.1 ± 955.2 | — | 210,468.3 | — |
| `Modify_nested` | `size=16` | 1,446.8 ± 8.0 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,505.7 ± 168.9 | — | 53,707.0 | — |
| `Modify_nested` | `size=4` | 685.2 ± 13.9 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,568.4 ± 14.2 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 257,518.0 ± 5,480.5 | — | 1,118,868.8 | — |
| `monocle_nested` | `size=16` | 2,608.1 ± 30.4 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 91,701.0 ± 451.1 | — | 430,208.7 | — |
| `monocle_nested` | `size=4` | 1,320.0 ± 110.3 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,800.1 ± 64.4 | — | 58,833.0 | — |
| `naive_nested` | `size=1024` | 21,684.4 ± 210.8 | — | 115,071.5 | — |
| `naive_nested` | `size=16` | 363.9 ± 2.4 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,206.8 ± 25.0 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 125.0 ± 2.2 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,320.8 ± 4.7 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,318.1 ± 3.4 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 22,834.9 ± 184.3 | — | 104,674.8 | — |
| `Modify_sparse` | `size=32` | 350.7 ± 8.9 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,642.5 ± 16.1 | — | 24,785.5 | — |
| `Modify_sparse` | `size=8` | 118.1 ± 0.4 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,817.2 ± 24.2 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 105,958.7 ± 368.8 | — | 523,146.4 | — |
| `monocle_sparse` | `size=32` | 1,026.9 ± 11.0 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,668.2 ± 963.3 | — | 166,686.1 | — |
| `monocle_sparse` | `size=8` | 354.9 ± 15.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 283.5 ± 0.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,929.8 ± 27.9 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 73.8 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,157.1 ± 3.6 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 24.8 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.3 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 33.8 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.6 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.9 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 104,830.8 ± 764.7 | — | 589,712.7 | — |
| `Cata` | `-` | 76,182.0 ± 1,111.7 | — | 197,568.5 | — |
| `Hylo` | `-` | 84,505.3 ± 1,364.5 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,334.1 ± 619.0 | — | 327,632.3 | — |
| `drosteCata` | `-` | 37,946.5 ± 145.2 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,627.6 ± 745.9 | — | 328,640.5 | — |
| `handAna` | `-` | 20,672.2 ± 403.0 | — | 163,816.1 | — |
| `handCata` | `-` | 12,707.9 ± 133.6 | — | 0.1 | — |
| `handHylo` | `-` | 9,895.9 ± 333.4 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.3 ± 0.2 | 28.0 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.5 ± 0.0 | 64.6 ± 0.7 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.2 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,956.6 ± 103.1 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 529.2 ± 1.4 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 48.9 ± 0.3 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,887.8 ± 39.1 | 31,102.9 ± 235.8 | 12,312.3 | 162,564.9 |
| `FoldPrices` | `size=64` | 154.3 ± 0.4 | 2,084.8 ± 10.7 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.5 ± 0.1 | 325.7 ± 2.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,200.7 ± 212.9 | 33,300.6 ± 276.5 | 36,897.1 | 176,890.8 |
| `Modify` | `size=64` | 839.5 ± 1.5 | 1,779.5 ± 7.2 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 94.4 ± 0.4 | 286.5 ± 4.1 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 94.2 ± 0.7 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.4 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 65.5 ± 0.2 | — | 400.0 | — |
| `handDrillGet` | `-` | 13.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 90.6 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 13.9 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.2 ± 0.6 | — | 520.0 | — |

