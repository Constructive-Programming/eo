# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `6e53b8574a3ff5c233303325e94c5d910496ad4b` · date: `2026-10-02` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.7 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.8 ± 0.1 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.7 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 9.0 ± 0.2 | 5.5 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 16.2 ± 0.3 | 14.0 ± 0.4 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 94.3 ± 2.7 | — | 720.0 | — |
| `ModifyCountry` | `-` | 211.8 ± 7.3 | — | 933.3 | — |
| `ModifyPartner` | `-` | 274.9 ± 10.9 | — | 984.0 | — |
| `ReadCountry` | `-` | 120.0 ± 5.1 | — | 520.0 | — |
| `ReadPartner` | `-` | 131.3 ± 6.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 186.7 ± 6.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 1,909.2 ± 66.8 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,192.8 ± 70.4 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,558.6 ± 87.5 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,162.1 ± 57.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,434.4 ± 63.1 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 709.6 ± 8.6 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 473.4 ± 17.1 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 246.2 ± 10.7 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 266.5 ± 4.1 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,126.0 ± 18.6 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,068.7 ± 27.8 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 37.6 ± 1.3 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 417.9 ± 49.4 | — | 568.0 | — |
| `handwritten_stream` | `-` | 124.0 ± 5.5 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 684.9 ± 19.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,136.0 ± 83.8 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 1,902.3 ± 45.0 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 526.7 ± 9.5 | — | 2,040.0 | — |
| `WideToJson` | `-` | 399.6 ± 18.3 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,105.5 ± 40.2 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,104.7 ± 83.7 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 868.3 ± 25.0 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,376.6 ± 21.9 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 127.5 ± 2.2 | — | 832.0 | — |
| `decode_native` | `-` | 14.8 ± 0.3 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 124.9 ± 1.9 | — | 832.0 | — |
| `encode_bridged` | `-` | 155.5 ± 4.3 | — | 1,234.7 | — |
| `encode_native` | `-` | 11.1 ± 0.8 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 151.2 ± 6.5 | — | 1,229.3 | — |
| `fieldGet_bridged` | `-` | 68.8 ± 1.0 | — | 432.0 | — |
| `fieldGet_native` | `-` | 72.0 ± 5.1 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 237.9 ± 7.0 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 112.3 ± 2.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 13.6 ± 0.3 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 14.9 ± 0.5 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 14.0 ± 0.6 | — | 0.0 | — |
| `foldMapDirect` | `-` | 13.6 ± 0.4 | — | 0.0 | — |
| `getCap` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.5 ± 0.1 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 19.8 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 21.1 ± 0.7 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 3.6 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.5 ± 0.2 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.2 ± 0.1 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 337.2 ± 9.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 336.9 ± 7.3 | — | 744.0 | — |
| `encode_hand` | `-` | 114.1 ± 1.7 | — | 768.0 | — |
| `encode_positional` | `-` | 4,183.8 ± 157.0 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 4,693.5 ± 105.1 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.8 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 16.9 ± 0.5 | — | 184.0 | — |
| `buildLens6` | `-` | 35.1 ± 4.4 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 16.6 ± 0.3 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.4 ± 0.2 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.0 ± 0.5 | — | 40.0 | — |
| `reuseLens3` | `-` | 33.6 ± 0.6 | — | 72.0 | — |
| `reuseLens6` | `-` | 73.6 ± 3.0 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 35.5 ± 1.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,329.2 ± 20.9 | 1,344.8 ± 29.8 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 157.2 ± 3.5 | 157.5 ± 4.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 13.5 ± 0.3 | 14.0 ± 0.8 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,044.4 ± 80.1 | 2,064.3 ± 103.8 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 148.7 ± 3.6 | 151.7 ± 5.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 19.3 ± 0.5 | 19.2 ± 0.4 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.8 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.5 ± 0.1 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.0 ± 0.1 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.5 ± 0.1 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.1 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.4 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.1 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.1 ± 0.1 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.6 ± 0.0 | 0.3 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 8.9 ± 0.3 | 4.7 ± 0.5 | 0.0 | 0.0 |
| `Get_6` | `-` | 17.2 ± 0.5 | 14.5 ± 0.5 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.6 ± 0.0 | 0.3 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.6 ± 0.1 | 2.7 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.7 ± 0.1 | 2.5 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 275,758.8 ± 4,909.1 | — | 1,066,670.4 | — |
| `cModifyId` | `size=64` | 32,031.5 ± 810.6 | — | 136,254.2 | — |
| `cModifyId` | `size=8` | 5,355.2 ± 186.2 | — | 20,696.1 | — |
| `cReadId` | `size=512` | 133,028.9 ± 6,973.6 | — | 797,909.8 | — |
| `cReadId` | `size=64` | 16,142.0 ± 480.5 | — | 101,288.5 | — |
| `cReadId` | `size=8` | 2,627.9 ± 64.8 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 128,720.7 ± 3,048.3 | — | 797,908.6 | — |
| `cReadStreet` | `size=64` | 16,178.4 ± 436.8 | — | 101,288.5 | — |
| `cReadStreet` | `size=8` | 2,652.5 ± 81.8 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 281,168.3 ± 9,007.4 | — | 1,066,624.0 | — |
| `cReplaceId` | `size=64` | 33,580.6 ± 1,704.8 | — | 136,206.6 | — |
| `cReplaceId` | `size=8` | 5,422.2 ± 162.0 | — | 20,656.1 | — |
| `cSumPrices` | `size=512` | 242,440.5 ± 8,774.3 | — | 1,240,708.9 | — |
| `cSumPrices` | `size=64` | 27,660.6 ± 794.5 | — | 157,582.0 | — |
| `cSumPrices` | `size=8` | 4,343.6 ± 73.0 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 132.1 ± 5.7 | — | 0.0 | — |
| `jMiss` | `size=64` | 130.8 ± 4.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 129.7 ± 4.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,380.8 ± 104.3 | — | 41,921.5 | — |
| `jModifyId` | `size=64` | 676.2 ± 6.3 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 147.5 ± 5.0 | — | 992.0 | — |
| `jReadId` | `size=512` | 30.0 ± 4.9 | — | 48.0 | — |
| `jReadId` | `size=64` | 32.9 ± 6.9 | — | 56.0 | — |
| `jReadId` | `size=8` | 30.5 ± 5.6 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 133.0 ± 7.8 | — | 144.0 | — |
| `jReadStreet` | `size=64` | 126.5 ± 3.0 | — | 144.0 | — |
| `jReadStreet` | `size=8` | 130.8 ± 5.1 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 5,291.3 ± 63.6 | — | 41,889.5 | — |
| `jReplaceId` | `size=64` | 670.5 ± 12.1 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 146.8 ± 2.9 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 58,899.7 ± 1,778.0 | — | 63,676.6 | — |
| `jSumPrices` | `size=64` | 7,499.5 ± 211.4 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 979.0 ± 23.0 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 73.3 ± 2.3 | — | 320.0 | — |
| `MapDrillModify` | `-` | 25.5 ± 0.7 | — | 216.0 | — |
| `MapGet` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 61.3 ± 1.6 | — | 216.0 | — |
| `handEnvUse` | `-` | 71.1 ± 1.3 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 23.2 ± 0.9 | — | 216.0 | — |
| `handMapGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 55.9 ± 1.6 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.5 ± 0.1 | 3.2 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 21.4 ± 0.5 | 19.7 ± 0.9 | 152.0 | 176.0 |
| `Replace` | `-` | 3.2 ± 0.1 | 3.2 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 13,486.6 ± 639.8 | — | 43,035.5 | — |
| `Fold_powerEach` | `size=256` | 2,473.3 ± 34.2 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 290.2 ± 34.5 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 48.6 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,824.7 ± 1,257.8 | — | 331,137.5 | — |
| `Modify_multiFocus` | `size=256` | 11,249.4 ± 198.5 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,516.7 ± 15.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 155.4 ± 4.8 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 26,492.5 ± 2,139.8 | — | 115,174.9 | — |
| `Modify_powerEach` | `size=256` | 6,149.4 ± 94.7 | — | 26,080.4 | — |
| `Modify_powerEach` | `size=32` | 798.4 ± 25.8 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 118.0 ± 1.9 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,404.5 ± 652.0 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 1,876.1 ± 40.1 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 243.9 ± 11.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 29.3 ± 0.4 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 3,994.2 ± 178.3 | — | 16,129.0 | — |
| `naive_sumQty` | `size=256` | 613.0 ± 10.2 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 52.4 ± 0.8 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 4.7 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 24.1 ± 0.7 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 100.9 ± 1.1 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 12.5 ± 0.4 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 20.8 ± 0.5 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.3 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 20.9 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.2 ± 0.3 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 160.6 ± 9.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 38.1 ± 1.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 696.5 ± 47.1 | — | 2,840.0 | — |
| `reuseUse` | `-` | 627.0 ± 17.7 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 14.2 ± 0.5 | 14.2 ± 0.6 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.8 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 34.5 ± 1.2 | 44.5 ± 1.4 | 160.0 | 304.0 |
| `Modify_6` | `-` | 77.1 ± 2.0 | 75.3 ± 1.6 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 12.6 ± 0.9 | 12.4 ± 0.8 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.5 ± 0.3 | 3.4 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.9 ± 0.1 | 6.9 ± 0.2 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 22,685.4 ± 1,193.6 | — | 97,421.4 | — |
| `ModifyNames` | `size=64` | 2,756.1 ± 95.6 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 381.6 ± 18.9 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 79.9 ± 1.5 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 79.4 ± 2.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 81.6 ± 3.7 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 23.6 ± 0.8 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 23.5 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 23.3 ± 0.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 74,146.7 ± 2,185.2 | — | 382,786.9 | — |
| `monocleModifyNames` | `size=64` | 6,755.2 ± 263.9 | — | 39,840.2 | — |
| `monocleModifyNames` | `size=8` | 1,007.9 ± 33.5 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 35,020.7 ± 1,424.3 | — | 169,071.1 | — |
| `monocleModifyStreet` | `size=64` | 4,447.4 ± 117.1 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 605.6 ± 26.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 17,410.6 ± 262.0 | — | 69,780.2 | — |
| `monocleReadStreet` | `size=64` | 2,841.2 ± 251.9 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 403.1 ± 75.1 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 43,150.9 ± 366.8 | — | 226,287.1 | — |
| `naiveModifyNames` | `size=64` | 5,464.5 ± 182.6 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 712.2 ± 25.5 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 33,568.4 ± 436.9 | — | 169,047.3 | — |
| `naiveModifyStreet` | `size=64` | 4,396.7 ± 139.9 | — | 20,880.1 | — |
| `naiveModifyStreet` | `size=8` | 605.9 ± 37.6 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 17,507.4 ± 173.5 | — | 69,780.2 | — |
| `naiveReadStreet` | `size=64` | 3,038.7 ± 314.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 295.4 ± 12.0 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 136,508.7 ± 5,885.7 | — | 613,949.4 | — |
| `Names` | `size=64` | 17,688.0 ± 813.8 | — | 78,777.9 | — |
| `Names` | `size=8` | 2,436.9 ± 107.9 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 154,436.9 ± 3,554.7 | — | 683,467.8 | — |
| `NamesIor` | `size=64` | 19,873.6 ± 531.2 | — | 87,487.4 | — |
| `NamesIor` | `size=8` | 2,554.1 ± 77.1 | — | 11,576.1 | — |
| `Street` | `size=512` | 659.8 ± 50.5 | — | 2,720.5 | — |
| `Street` | `size=64` | 659.6 ± 52.2 | — | 2,720.1 | — |
| `Street` | `size=8` | 613.3 ± 8.4 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 615.5 ± 16.7 | — | 2,736.5 | — |
| `StreetIor` | `size=64` | 623.9 ± 13.8 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 609.6 ± 8.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 136,137.3 ± 5,706.7 | — | 613,909.1 | — |
| `directNames` | `size=64` | 16,900.4 ± 271.8 | — | 77,177.8 | — |
| `directNames` | `size=8` | 2,349.1 ± 89.2 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 635.5 ± 18.9 | — | 2,728.5 | — |
| `directStreet` | `size=64` | 652.9 ± 60.7 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 653.2 ± 37.3 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 135,868.0 ± 3,572.9 | — | 609,804.9 | — |
| `hcursorNames` | `size=64` | 17,339.3 ± 651.4 | — | 77,257.8 | — |
| `hcursorNames` | `size=8` | 2,357.0 ± 79.5 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 711.7 ± 53.2 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 691.7 ± 22.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 673.7 ± 11.5 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 189,274.0 ± 2,993.8 | — | 1,121,735.2 | — |
| `monocleNames` | `size=64` | 18,595.4 ± 535.2 | — | 132,767.0 | — |
| `monocleNames` | `size=8` | 3,119.9 ± 56.7 | — | 19,477.4 | — |
| `monocleStreet` | `size=512` | 146,025.2 ± 1,402.9 | — | 908,002.1 | — |
| `monocleStreet` | `size=64` | 15,821.4 ± 378.3 | — | 113,809.4 | — |
| `monocleStreet` | `size=8` | 2,700.5 ± 75.5 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 163,938.1 ± 7,361.0 | — | 965,246.2 | — |
| `naiveNames` | `size=64` | 16,912.1 ± 305.8 | — | 120,825.5 | — |
| `naiveNames` | `size=8` | 2,810.1 ± 59.9 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 149,523.8 ± 2,282.8 | — | 908,009.8 | — |
| `naiveStreet` | `size=64` | 16,091.2 ± 453.9 | — | 113,801.4 | — |
| `naiveStreet` | `size=8` | 2,727.6 ± 64.0 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 5,497.2 ± 83.6 | — | 42,028.9 | — |
| `ModifyStreet` | `size=64` | 830.7 ± 16.3 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 213.8 ± 11.1 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 130.0 ± 5.6 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 128.9 ± 4.4 | — | 109.3 | — |
| `ReadStreet` | `size=8` | 125.9 ± 2.3 | — | 90.7 | — |
| `SumPrices` | `size=512` | 59,361.5 ± 1,761.7 | — | 63,711.9 | — |
| `SumPrices` | `size=64` | 7,662.4 ± 375.2 | — | 8,120.9 | — |
| `SumPrices` | `size=8` | 998.1 ± 32.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 106,445.4 ± 4,506.6 | — | 333,473.6 | — |
| `monocleModifyStreet` | `size=64` | 12,360.4 ± 434.7 | — | 30,081.2 | — |
| `monocleModifyStreet` | `size=8` | 1,988.9 ± 33.4 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 55,727.5 ± 2,747.4 | — | 193,199.7 | — |
| `monocleReadStreet` | `size=64` | 6,846.0 ± 225.3 | — | 24,704.7 | — |
| `monocleReadStreet` | `size=8` | 1,106.9 ± 22.5 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 302,491.2 ± 18,291.5 | — | 1,178,338.9 | — |
| `monocleSumPrices` | `size=64` | 10,097.4 ± 333.6 | — | 45,841.0 | — |
| `monocleSumPrices` | `size=8` | 1,620.9 ± 59.8 | — | 6,445.4 | — |
| `naiveModifyStreet` | `size=512` | 107,342.7 ± 4,232.6 | — | 333,466.3 | — |
| `naiveModifyStreet` | `size=64` | 12,129.9 ± 323.8 | — | 30,057.2 | — |
| `naiveModifyStreet` | `size=8` | 2,015.1 ± 53.0 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 55,926.8 ± 2,328.4 | — | 193,199.9 | — |
| `naiveReadStreet` | `size=64` | 7,037.7 ± 359.0 | — | 24,704.7 | — |
| `naiveReadStreet` | `size=8` | 1,131.7 ± 50.5 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 58,612.2 ± 1,852.9 | — | 230,090.2 | — |
| `naiveSumPrices` | `size=64` | 7,396.0 ± 175.9 | — | 29,336.8 | — |
| `naiveSumPrices` | `size=8` | 1,164.6 ± 19.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 25,173.3 ± 782.5 | — | 445.0 | — |
| `nativeReadStreet` | `size=64` | 3,200.1 ± 116.8 | — | 424.3 | — |
| `nativeReadStreet` | `size=8` | 569.7 ± 14.8 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 43,656.3 ± 2,038.0 | — | 86,268.1 | — |
| `nativeSumPrices` | `size=64` | 5,282.6 ± 229.6 | — | 10,920.6 | — |
| `nativeSumPrices` | `size=8` | 748.9 ± 33.0 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 113,326.9 ± 2,754.6 | — | 624,378.5 | — |
| `TransformDeep` | `n=512` | 10,667.5 ± 288.8 | — | 57,361.1 | — |
| `TransformDeep` | `n=64` | 1,196.8 ± 27.1 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 108,826.1 ± 7,367.5 | 114,486.6 ± 3,342.9 | 655,343.2 | 753,699.3 |
| `TransformExpr` | `n=512` | 13,313.6 ± 265.0 | 10,687.4 ± 179.1 | 81,825.4 | 69,585.1 |
| `TransformExpr` | `n=64` | 1,701.4 ± 23.1 | 1,875.6 ± 89.0 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 145,025.4 ± 3,131.3 | — | 786,609.6 | — |
| `UniverseDeep` | `n=512` | 14,161.1 ± 537.4 | — | 98,377.4 | — |
| `UniverseDeep` | `n=64` | 1,580.6 ± 56.9 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 145,392.7 ± 3,046.4 | 3,858,618.7 ± 208,771.0 | 786,417.8 | 4,688,483.2 |
| `UniverseExpr` | `n=512` | 14,006.6 ± 308.2 | 256,141.0 ± 7,774.9 | 98,185.4 | 475,018.2 |
| `UniverseExpr` | `n=64` | 1,597.6 ± 81.0 | 22,972.4 ± 527.2 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 200,009.6 ± 6,961.7 | 4,183,491.0 ± 222,157.7 | 786,457.6 | 6,490,581.5 |
| `UniverseJson` | `n=512` | 20,510.5 ± 519.2 | 298,099.7 ± 10,105.5 | 98,186.1 | 699,926.4 |
| `UniverseJson` | `n=64` | 2,520.9 ± 107.6 | 26,607.2 ± 691.3 | 12,168.0 | 73,208.5 |
| `visitorTransformDeep` | `n=4096` | 28,478.6 ± 730.9 | — | 163,876.7 | — |
| `visitorTransformDeep` | `n=512` | 3,335.8 ± 85.9 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 388.6 ± 11.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 53,102.5 ± 716.5 | — | 360,462.7 | — |
| `visitorTransformExpr` | `n=512` | 5,965.5 ± 159.6 | — | 45,032.6 | — |
| `visitorTransformExpr` | `n=64` | 809.7 ± 21.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 48,077.1 ± 783.6 | — | 196,699.0 | — |
| `visitorUniverseDeep` | `n=512` | 5,400.2 ± 111.5 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 607.3 ± 20.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,303.4 ± 1,145.9 | — | 196,654.1 | — |
| `visitorUniverseExpr` | `n=512` | 6,284.3 ± 162.1 | — | 24,584.6 | — |
| `visitorUniverseExpr` | `n=64` | 605.1 ± 10.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 119,428.0 ± 36,166.3 | — | 374,558.9 | — |
| `visitorUniverseJson` | `n=512` | 21,600.7 ± 500.2 | — | 40,954.2 | — |
| `visitorUniverseJson` | `n=64` | 1,725.3 ± 54.4 | — | 4,424.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 9,886.9 ± 200.8 | — | 41,412.7 | — |
| `Modify_powerEach` | `size=16` | 171.8 ± 9.4 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,661.3 ± 138.5 | — | 10,688.4 | — |
| `Modify_powerEach` | `size=4` | 76.9 ± 2.2 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 41,538.5 ± 1,898.3 | — | 164,356.0 | — |
| `Modify_powerEach` | `size=64` | 551.6 ± 11.8 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 49,643.0 ± 838.4 | — | 279,425.7 | — |
| `monocle_powerEach` | `size=16` | 504.5 ± 10.4 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 16,763.3 ± 388.6 | — | 107,338.6 | — |
| `monocle_powerEach` | `size=4` | 130.9 ± 5.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 194,302.6 ± 4,965.9 | — | 967,868.4 | — |
| `monocle_powerEach` | `size=64` | 2,150.6 ± 37.8 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,159.9 ± 108.3 | — | 28,730.4 | — |
| `naive_powerEach` | `size=16` | 71.4 ± 1.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,262.8 ± 16.6 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 19.5 ± 0.7 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,758.9 ± 760.9 | — | 114,783.0 | — |
| `naive_powerEach` | `size=64` | 336.2 ± 4.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 49,951.6 ± 1,641.6 | — | 210,533.6 | — |
| `Modify_nested` | `size=16` | 970.4 ± 27.2 | — | 4,744.0 | — |
| `Modify_nested` | `size=256` | 14,139.5 ± 281.2 | — | 53,696.9 | — |
| `Modify_nested` | `size=4` | 441.6 ± 12.4 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 3,800.5 ± 170.3 | — | 14,616.4 | — |
| `monocle_nested` | `size=1024` | 221,339.5 ± 6,741.5 | — | 1,118,802.9 | — |
| `monocle_nested` | `size=16` | 2,462.5 ± 111.3 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 71,544.4 ± 1,584.1 | — | 430,203.3 | — |
| `monocle_nested` | `size=4` | 908.2 ± 24.2 | — | 5,536.0 | — |
| `monocle_nested` | `size=64` | 8,092.0 ± 268.3 | — | 58,859.5 | — |
| `naive_nested` | `size=1024` | 19,826.4 ± 650.6 | — | 115,068.1 | — |
| `naive_nested` | `size=16` | 309.9 ± 11.5 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,900.5 ± 120.3 | — | 29,019.1 | — |
| `naive_nested` | `size=4` | 95.0 ± 3.3 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,230.1 ± 26.1 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,238.7 ± 95.2 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 19,756.0 ± 405.9 | — | 104,687.9 | — |
| `Modify_sparse` | `size=32` | 298.2 ± 7.4 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,004.9 ± 104.0 | — | 24,785.4 | — |
| `Modify_sparse` | `size=8` | 87.8 ± 2.5 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,997.1 ± 136.1 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 97,801.6 ± 8,555.6 | — | 523,143.7 | — |
| `monocle_sparse` | `size=32` | 1,131.4 ± 27.3 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 27,484.7 ± 720.6 | — | 166,680.5 | — |
| `monocle_sparse` | `size=8` | 294.5 ± 20.7 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 323.6 ± 9.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 3,810.4 ± 94.1 | — | 24,611.2 | — |
| `naive_sparse` | `size=32` | 82.7 ± 1.6 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,163.7 ± 41.9 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 17.0 ± 0.4 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.1 ± 0.1 | 2.2 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.1 | 2.3 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.1 | 2.3 ± 0.1 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.1 ± 0.1 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.4 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 32.3 ± 0.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.0 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 9.8 ± 0.2 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 83,808.2 ± 2,297.0 | — | 589,712.6 | — |
| `Cata` | `-` | 66,151.1 ± 1,339.7 | — | 197,568.5 | — |
| `Hylo` | `-` | 69,268.7 ± 6,246.8 | — | 295,848.5 | — |
| `drosteAna` | `-` | 47,082.2 ± 3,478.9 | — | 327,632.3 | — |
| `drosteCata` | `-` | 41,900.7 ± 1,702.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 49,002.3 ± 3,275.8 | — | 328,640.3 | — |
| `handAna` | `-` | 29,080.8 ± 1,333.6 | — | 163,816.2 | — |
| `handCata` | `-` | 9,224.0 ± 178.4 | — | 0.1 | — |
| `handHylo` | `-` | 6,835.2 ± 180.0 | — | 0.0 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.3 ± 0.2 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 8.6 ± 0.2 | 21.7 ± 1.0 | 72.0 | 168.0 |
| `Modify_6` | `-` | 17.9 ± 0.7 | 41.5 ± 1.4 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.0 ± 0.2 | 3.1 ± 0.4 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,042.5 ± 67.8 | — | 20,200.6 | — |
| `FoldNested` | `size=64` | 465.0 ± 7.9 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 40.5 ± 6.1 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,699.9 ± 21.9 | 24,631.9 ± 313.5 | 12,312.3 | 162,563.9 |
| `FoldPrices` | `size=64` | 149.8 ± 1.6 | 2,184.0 ± 16.8 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 18.5 ± 0.4 | 265.5 ± 9.4 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 6,353.5 ± 104.7 | 28,880.7 ± 400.6 | 36,897.0 | 176,887.0 |
| `Modify` | `size=64` | 723.0 ± 17.5 | 2,025.0 ± 37.9 | 4,640.0 | 14,408.1 |
| `Modify` | `size=8` | 77.7 ± 2.6 | 240.9 ± 8.8 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 6.5 ± 0.2 | — | 0.0 | — |
| `DrillModify` | `-` | 76.6 ± 4.8 | — | 472.0 | — |
| `ServiceGet` | `-` | 3.5 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 68.8 ± 0.4 | — | 400.0 | — |
| `handDrillGet` | `-` | 10.7 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 92.5 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 10.3 ± 0.3 | — | 120.0 | — |
| `handServiceReplace` | `-` | 85.0 ± 5.3 | — | 520.0 | — |

