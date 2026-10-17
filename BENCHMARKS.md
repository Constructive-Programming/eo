# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `611ee84ac32dca71ee77225ee4f56e0f0fcc8edc` · date: `2026-10-17` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.7 ± 2.3 | 9.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 27.4 ± 1.7 | 23.8 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 159.9 ± 1.1 | — | 720.0 | — |
| `ModifyCountry` | `-` | 298.9 ± 7.8 | — | 928.0 | — |
| `ModifyPartner` | `-` | 396.8 ± 7.3 | — | 984.0 | — |
| `ReadCountry` | `-` | 178.0 ± 4.1 | — | 520.0 | — |
| `ReadPartner` | `-` | 211.2 ± 6.0 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 309.4 ± 5.7 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,627.9 ± 33.5 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,915.2 ± 27.4 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,933.7 ± 83.0 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,670.9 ± 14.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,039.8 ± 29.8 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 963.5 ± 55.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 618.7 ± 12.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 406.6 ± 5.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 436.4 ± 1.5 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,360.7 ± 29.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,342.5 ± 8.8 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 54.0 ± 0.1 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 624.3 ± 9.7 | — | 568.0 | — |
| `handwritten_stream` | `-` | 159.2 ± 1.2 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 670.0 ± 14.6 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,959.7 ± 37.7 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,829.5 ± 47.8 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 849.6 ± 6.1 | — | 2,040.0 | — |
| `WideToJson` | `-` | 666.8 ± 6.9 | — | 1,482.7 | — |
| `naiveClickToAvro` | `-` | 1,459.6 ± 9.5 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,968.4 ± 49.8 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 990.6 ± 18.7 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,892.6 ± 16.5 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 191.9 ± 0.6 | — | 832.0 | — |
| `decode_native` | `-` | 17.2 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 187.6 ± 0.7 | — | 832.0 | — |
| `encode_bridged` | `-` | 256.5 ± 8.7 | — | 1,245.3 | — |
| `encode_native` | `-` | 11.8 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 260.0 ± 0.9 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 101.5 ± 1.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.9 ± 1.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 395.3 ± 7.5 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 168.2 ± 2.4 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.1 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.5 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.3 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.8 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.1 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.8 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.2 ± 0.0 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 522.1 ± 10.1 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 536.1 ± 10.2 | — | 744.0 | — |
| `encode_hand` | `-` | 140.4 ± 1.3 | — | 768.0 | — |
| `encode_positional` | `-` | 6,395.1 ± 34.0 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,829.6 ± 50.5 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 6.0 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.7 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 38.9 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.2 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.5 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.1 ± 0.7 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 57.5 ± 0.3 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,236.1 ± 13.5 | 1,242.3 ± 8.3 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 157.7 ± 0.7 | 157.2 ± 0.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.0 | 21.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,127.0 ± 47.4 | 2,128.8 ± 27.4 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 158.3 ± 0.9 | 157.0 ± 0.5 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.9 ± 0.2 | 26.4 ± 0.0 | 216.0 | 216.0 |

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
| `handLensModify` | `-` | 2.9 ± 0.1 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.1 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.2 ± 0.0 | 8.8 ± 1.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.5 ± 0.0 | 28.2 ± 0.1 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 407,957.8 ± 3,372.6 | — | 1,066,764.0 | — |
| `cModifyId` | `size=64` | 52,392.4 ± 927.7 | — | 136,293.9 | — |
| `cModifyId` | `size=8` | 8,561.8 ± 95.2 | — | 20,738.8 | — |
| `cReadId` | `size=512` | 209,699.4 ± 2,083.6 | — | 797,931.6 | — |
| `cReadId` | `size=64` | 25,764.7 ± 307.3 | — | 101,293.8 | — |
| `cReadId` | `size=8` | 3,976.7 ± 75.9 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 216,891.5 ± 5,622.4 | — | 797,933.7 | — |
| `cReadStreet` | `size=64` | 26,137.5 ± 1,155.3 | — | 101,294.3 | — |
| `cReadStreet` | `size=8` | 3,974.3 ± 71.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 410,028.5 ± 12,170.1 | — | 1,066,706.5 | — |
| `cReplaceId` | `size=64` | 51,764.7 ± 602.4 | — | 136,245.6 | — |
| `cReplaceId` | `size=8` | 8,549.4 ± 74.7 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 337,352.5 ± 2,226.5 | — | 1,240,736.8 | — |
| `cSumPrices` | `size=64` | 42,047.5 ± 636.3 | — | 157,037.1 | — |
| `cSumPrices` | `size=8` | 6,226.2 ± 203.4 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 183.6 ± 0.4 | — | 0.1 | — |
| `jMiss` | `size=64` | 178.6 ± 8.1 | — | 0.0 | — |
| `jMiss` | `size=8` | 173.1 ± 8.5 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,835.8 ± 36.4 | — | 41,921.1 | — |
| `jModifyId` | `size=64` | 494.6 ± 9.1 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 113.6 ± 0.7 | — | 976.0 | — |
| `jReadId` | `size=512` | 35.4 ± 2.1 | — | 56.0 | — |
| `jReadId` | `size=64` | 34.2 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 34.4 ± 0.7 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 200.7 ± 8.7 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 195.1 ± 10.6 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 199.8 ± 9.7 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 3,841.6 ± 31.1 | — | 41,889.1 | — |
| `jReplaceId` | `size=64` | 487.5 ± 9.9 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 109.7 ± 1.7 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 84,645.9 ± 707.2 | — | 63,662.6 | — |
| `jSumPrices` | `size=64` | 10,500.4 ± 305.7 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,433.5 ± 11.7 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 86.0 ± 1.1 | — | 320.0 | — |
| `MapDrillModify` | `-` | 40.0 ± 1.6 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 66.9 ± 5.0 | — | 216.0 | — |
| `handEnvUse` | `-` | 86.6 ± 2.0 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.5 ± 1.1 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 60.9 ± 0.6 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.2 ± 0.1 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.3 ± 0.4 | 29.9 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.6 ± 0.1 | 3.6 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,466.1 ± 551.2 | — | 43,035.8 | — |
| `Fold_powerEach` | `size=256` | 3,186.5 ± 48.4 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 377.0 ± 2.0 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 85.3 ± 0.4 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 50,588.8 ± 2,276.5 | — | 331,117.1 | — |
| `Modify_multiFocus` | `size=256` | 11,378.0 ± 65.7 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,375.0 ± 13.2 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 221.5 ± 8.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,108.0 ± 615.0 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 8,219.8 ± 64.1 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 933.9 ± 132.5 | — | 3,168.0 | — |
| `Modify_powerEach` | `size=4` | 195.3 ± 7.8 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,365.3 ± 46.9 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,075.2 ± 9.6 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 251.4 ± 0.5 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.5 ± 0.4 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,089.6 ± 46.0 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 734.6 ± 39.3 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 58.3 ± 0.2 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.5 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.7 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 123.2 ± 0.8 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.9 ± 0.2 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.7 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.2 ± 0.3 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.4 ± 0.5 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 159.8 ± 3.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.6 ± 4.5 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,115.9 ± 12.2 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,053.0 ± 10.7 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.9 ± 0.3 | 21.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.2 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 57.5 ± 0.3 | 70.9 ± 1.0 | 160.0 | 304.0 |
| `Modify_6` | `-` | 135.3 ± 0.3 | 120.5 ± 0.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.7 ± 0.1 | 16.3 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.6 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 8.1 ± 0.1 | 7.3 ± 0.2 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,424.7 ± 1,708.5 | — | 97,423.8 | — |
| `ModifyNames` | `size=64` | 4,056.6 ± 100.5 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 540.5 ± 7.8 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 110.9 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 111.1 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.4 ± 0.5 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.3 ± 0.8 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,663.3 ± 925.8 | — | 382,775.5 | — |
| `monocleModifyNames` | `size=64` | 9,028.5 ± 33.0 | — | 39,856.3 | — |
| `monocleModifyNames` | `size=8` | 1,405.2 ± 5.2 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,899.7 ± 369.2 | — | 169,079.6 | — |
| `monocleModifyStreet` | `size=64` | 5,913.8 ± 185.5 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 918.9 ± 9.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,141.1 ± 180.7 | — | 69,783.7 | — |
| `monocleReadStreet` | `size=64` | 3,221.1 ± 14.5 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 477.0 ± 5.3 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,574.1 ± 150.0 | — | 226,296.1 | — |
| `naiveModifyNames` | `size=64` | 7,530.2 ± 186.3 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,095.3 ± 5.3 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,217.9 ± 417.8 | — | 169,057.0 | — |
| `naiveModifyStreet` | `size=64` | 5,863.9 ± 189.8 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 923.6 ± 5.1 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,062.0 ± 101.2 | — | 69,783.4 | — |
| `naiveReadStreet` | `size=64` | 3,226.1 ± 15.3 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 474.4 ± 4.0 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 249,965.7 ± 4,150.0 | — | 626,352.4 | — |
| `Names` | `size=64` | 30,504.7 ± 450.9 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,157.4 ± 134.0 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 265,455.5 ± 3,792.0 | — | 695,868.8 | — |
| `NamesIor` | `size=64` | 33,398.3 ± 455.7 | — | 86,979.5 | — |
| `NamesIor` | `size=8` | 4,541.0 ± 87.2 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,058.6 ± 16.2 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,067.0 ± 27.0 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,064.3 ± 13.1 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,063.8 ± 10.7 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,063.8 ± 28.5 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,095.1 ± 35.6 | — | 2,736.0 | — |
| `directNames` | `size=512` | 246,777.7 ± 3,769.8 | — | 613,997.8 | — |
| `directNames` | `size=64` | 30,379.8 ± 245.6 | — | 77,707.4 | — |
| `directNames` | `size=8` | 4,093.7 ± 94.5 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,061.4 ± 6.9 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,050.4 ± 16.2 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,031.3 ± 16.5 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 249,779.5 ± 6,962.6 | — | 605,792.2 | — |
| `hcursorNames` | `size=64` | 29,845.4 ± 403.9 | — | 77,791.8 | — |
| `hcursorNames` | `size=8` | 4,131.2 ± 46.1 | — | 10,624.1 | — |
| `hcursorStreet` | `size=512` | 1,118.7 ± 13.1 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,154.1 ± 15.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,137.1 ± 34.8 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 224,927.4 ± 2,325.3 | — | 1,121,764.4 | — |
| `monocleNames` | `size=64` | 24,121.3 ± 400.0 | — | 132,757.2 | — |
| `monocleNames` | `size=8` | 3,664.5 ± 32.1 | — | 19,461.4 | — |
| `monocleStreet` | `size=512` | 179,694.7 ± 1,111.2 | — | 902,574.1 | — |
| `monocleStreet` | `size=64` | 21,017.2 ± 386.9 | — | 112,772.5 | — |
| `monocleStreet` | `size=8` | 3,159.9 ± 9.7 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 193,319.5 ± 753.3 | — | 965,271.2 | — |
| `naiveNames` | `size=64` | 22,943.4 ± 82.5 | — | 120,837.4 | — |
| `naiveNames` | `size=8` | 3,373.9 ± 16.4 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 180,810.2 ± 4,317.5 | — | 908,036.1 | — |
| `naiveStreet` | `size=64` | 21,054.8 ± 309.5 | — | 113,436.5 | — |
| `naiveStreet` | `size=8` | 3,192.6 ± 30.0 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,341.4 ± 49.2 | — | 42,011.8 | — |
| `ModifyStreet` | `size=64` | 690.0 ± 5.6 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 304.5 ± 6.3 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 197.4 ± 2.4 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 198.9 ± 3.0 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 199.1 ± 4.5 | — | 109.3 | — |
| `SumPrices` | `size=512` | 85,383.0 ± 509.0 | — | 63,714.2 | — |
| `SumPrices` | `size=64` | 10,438.9 ± 54.9 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,434.2 ± 14.9 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 157,578.6 ± 608.0 | — | 333,571.7 | — |
| `monocleModifyStreet` | `size=64` | 18,873.2 ± 228.1 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,163.8 ± 9.4 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 88,097.7 ± 498.7 | — | 193,227.4 | — |
| `monocleReadStreet` | `size=64` | 11,344.8 ± 210.5 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,795.1 ± 32.1 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 416,937.0 ± 3,854.2 | — | 1,184,582.6 | — |
| `monocleSumPrices` | `size=64` | 15,523.7 ± 69.8 | — | 45,833.6 | — |
| `monocleSumPrices` | `size=8` | 2,401.0 ± 77.4 | — | 6,333.4 | — |
| `naiveModifyStreet` | `size=512` | 158,845.6 ± 2,398.5 | — | 333,564.7 | — |
| `naiveModifyStreet` | `size=64` | 18,788.8 ± 104.0 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,150.4 ± 11.0 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 87,935.1 ± 690.1 | — | 193,227.3 | — |
| `naiveReadStreet` | `size=64` | 11,643.3 ± 111.3 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,769.3 ± 8.3 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,441.1 ± 570.2 | — | 230,119.3 | — |
| `naiveSumPrices` | `size=64` | 11,856.1 ± 257.4 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,909.9 ± 18.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 32,815.5 ± 348.1 | — | 451.5 | — |
| `nativeReadStreet` | `size=64` | 4,259.1 ± 93.7 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 816.3 ± 18.9 | — | 413.4 | — |
| `nativeSumPrices` | `size=512` | 64,634.7 ± 2,458.2 | — | 86,272.4 | — |
| `nativeSumPrices` | `size=64` | 7,636.5 ± 38.7 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,149.8 ± 22.8 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 119,683.6 ± 1,395.5 | — | 624,383.1 | — |
| `TransformDeep` | `n=512` | 11,710.9 ± 49.0 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,425.6 ± 4.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 128,207.0 ± 1,896.3 | 164,392.0 ± 1,005.2 | 655,357.3 | 753,735.6 |
| `TransformExpr` | `n=512` | 15,911.4 ± 72.5 | 15,400.4 ± 175.2 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,998.3 ± 13.4 | 2,475.6 ± 12.8 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 106,720.6 ± 6,021.9 | — | 786,581.7 | — |
| `UniverseDeep` | `n=512` | 15,292.8 ± 71.7 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,879.9 ± 6.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 100,045.8 ± 2,489.9 | 2,956,668.1 ± 188,510.7 | 786,384.8 | 4,687,828.2 |
| `UniverseExpr` | `n=512` | 14,861.0 ± 48.7 | 200,704.7 ± 6,786.8 | 98,185.5 | 475,012.6 |
| `UniverseExpr` | `n=64` | 1,802.5 ± 14.4 | 17,917.4 ± 644.6 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 215,629.2 ± 2,017.8 | 3,241,679.6 ± 177,186.4 | 786,468.9 | 6,489,899.3 |
| `UniverseJson` | `n=512` | 24,726.4 ± 62.8 | 237,714.0 ± 1,782.9 | 98,186.5 | 699,920.3 |
| `UniverseJson` | `n=64` | 2,976.2 ± 4.2 | 22,971.7 ± 981.6 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,247.5 ± 173.4 | — | 163,886.8 | — |
| `visitorTransformDeep` | `n=512` | 4,911.6 ± 34.0 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 427.4 ± 1.8 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,615.8 ± 665.1 | — | 360,471.0 | — |
| `visitorTransformExpr` | `n=512` | 7,568.7 ± 58.5 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 989.0 ± 7.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,741.9 ± 421.9 | — | 196,703.8 | — |
| `visitorUniverseDeep` | `n=512` | 6,494.0 ± 48.6 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 748.4 ± 5.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 53,030.1 ± 425.4 | — | 196,654.6 | — |
| `visitorUniverseExpr` | `n=512` | 6,569.6 ± 93.3 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 799.0 ± 1.1 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 162,873.1 ± 4,924.3 | — | 447,926.5 | — |
| `visitorUniverseJson` | `n=512` | 21,531.6 ± 294.9 | — | 53,194.2 | — |
| `visitorUniverseJson` | `n=64` | 2,370.2 ± 199.8 | — | 4,664.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,239.9 ± 90.1 | — | 41,413.8 | — |
| `Modify_powerEach` | `size=16` | 257.0 ± 1.0 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,025.5 ± 46.9 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 124.1 ± 1.5 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,660.3 ± 1,700.0 | — | 164,372.1 | — |
| `Modify_powerEach` | `size=64` | 797.6 ± 8.0 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 60,929.2 ± 431.9 | — | 279,433.5 | — |
| `monocle_powerEach` | `size=16` | 604.2 ± 3.0 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,578.4 ± 169.9 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 211.2 ± 2.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 178,170.0 ± 2,824.7 | — | 967,842.1 | — |
| `monocle_powerEach` | `size=64` | 2,174.1 ± 21.2 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,747.4 ± 67.0 | — | 28,731.2 | — |
| `naive_powerEach` | `size=16` | 101.1 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,616.5 ± 5.9 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.1 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,980.7 ± 115.4 | — | 114,788.0 | — |
| `naive_powerEach` | `size=64` | 396.4 ± 0.7 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 74,123.1 ± 1,348.4 | — | 210,556.3 | — |
| `Modify_nested` | `size=16` | 1,454.9 ± 18.3 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,635.7 ± 100.9 | — | 53,739.0 | — |
| `Modify_nested` | `size=4` | 677.8 ± 14.3 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,587.1 ± 26.1 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 251,572.6 ± 2,323.5 | — | 1,118,858.0 | — |
| `monocle_nested` | `size=16` | 2,687.3 ± 100.6 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 93,593.8 ± 883.3 | — | 430,209.2 | — |
| `monocle_nested` | `size=4` | 1,289.9 ± 81.0 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,897.0 ± 197.7 | — | 58,859.6 | — |
| `naive_nested` | `size=1024` | 21,747.6 ± 154.2 | — | 115,071.6 | — |
| `naive_nested` | `size=16` | 363.6 ± 2.5 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,246.4 ± 22.8 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 126.5 ± 2.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,325.1 ± 8.0 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,319.4 ± 4.3 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,496.9 ± 940.2 | — | 104,683.4 | — |
| `Modify_sparse` | `size=32` | 345.0 ± 1.0 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,694.7 ± 33.1 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 119.2 ± 0.3 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,900.1 ± 76.5 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 112,515.2 ± 11,292.7 | — | 523,150.4 | — |
| `monocle_sparse` | `size=32` | 1,054.3 ± 12.0 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 32,961.5 ± 247.0 | — | 166,685.5 | — |
| `monocle_sparse` | `size=8` | 338.3 ± 2.6 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 288.1 ± 1.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,026.2 ± 45.3 | — | 24,612.2 | — |
| `naive_sparse` | `size=32` | 74.3 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,175.6 ± 7.3 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.0 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.4 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.5 ± 0.1 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.1 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.3 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 96,454.8 ± 1,295.0 | — | 589,712.7 | — |
| `Cata` | `-` | 70,303.6 ± 1,214.1 | — | 197,568.5 | — |
| `Hylo` | `-` | 90,264.1 ± 1,104.2 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,582.9 ± 429.5 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,082.5 ± 147.6 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,043.2 ± 774.5 | — | 328,640.5 | — |
| `handAna` | `-` | 21,043.9 ± 250.6 | — | 163,816.1 | — |
| `handCata` | `-` | 12,599.1 ± 20.9 | — | 0.1 | — |
| `handHylo` | `-` | 9,538.6 ± 43.2 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.5 ± 0.0 | 2.5 ± 0.1 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.3 ± 0.0 | 28.4 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 24.7 ± 0.8 | 64.3 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.7 ± 0.1 | 3.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,046.6 ± 105.2 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 532.6 ± 2.1 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 52.4 ± 4.9 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,908.3 ± 41.5 | 31,473.3 ± 271.3 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 158.7 ± 1.8 | 2,113.1 ± 10.8 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 26.0 ± 0.3 | 324.2 ± 3.4 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,298.0 ± 258.0 | 33,915.8 ± 536.2 | 36,897.2 | 176,891.4 |
| `Modify` | `size=64` | 845.1 ± 1.5 | 1,848.4 ± 19.3 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 95.5 ± 0.3 | 285.9 ± 1.8 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 94.0 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 66.1 ± 0.8 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.3 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 91.8 ± 0.8 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.2 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 76.3 ± 0.6 | — | 520.0 | — |

