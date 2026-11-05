# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `72e88e27b2bdb7b04da7c142eee0c53731c62c48` · date: `2026-11-05` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.4 ± 0.1 | 10.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.1 ± 0.2 | 25.9 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 157.8 ± 0.7 | — | 720.0 | — |
| `ModifyCountry` | `-` | 286.7 ± 3.5 | — | 933.3 | — |
| `ModifyPartner` | `-` | 355.8 ± 4.5 | — | 989.3 | — |
| `ReadCountry` | `-` | 178.7 ± 2.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 211.6 ± 5.8 | — | 485.3 | — |
| `SliceGraftPayload` | `-` | 313.2 ± 6.5 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,698.4 ± 22.2 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,108.8 ± 94.8 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 4,943.1 ± 62.6 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,685.7 ± 16.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,020.7 ± 35.9 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 881.4 ± 16.4 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 577.9 ± 8.9 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 415.3 ± 5.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 446.0 ± 3.6 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,361.7 ± 8.2 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,359.0 ± 14.4 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 55.4 ± 0.5 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 656.6 ± 10.2 | — | 568.0 | — |
| `handwritten_stream` | `-` | 156.4 ± 1.2 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 707.3 ± 32.6 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,049.8 ± 37.3 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,767.5 ± 47.8 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 720.6 ± 22.9 | — | 2,040.0 | — |
| `WideToJson` | `-` | 598.5 ± 19.3 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,407.8 ± 9.9 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,972.8 ± 38.3 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 923.3 ± 26.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,870.2 ± 28.2 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 354.9 ± 2.4 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 344.8 ± 30.4 | — | 765.4 | — |
| `circeStrictFull` | `-` | 352.3 ± 3.1 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 3.1 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 719.2 ± 11.0 | — | 2,706.7 | — |
| `jsoniterDefaultsFull` | `-` | 1,214.2 ± 35.7 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 332.1 ± 4.9 | — | 640.0 | — |
| `jsoniterStrictFull` | `-` | 1,183.7 ± 18.2 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 409.0 ± 5.3 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 700.4 ± 25.1 | — | 984.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 189.4 ± 2.7 | — | 832.0 | — |
| `decode_native` | `-` | 17.4 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 183.1 ± 1.6 | — | 832.0 | — |
| `encode_bridged` | `-` | 225.9 ± 7.1 | — | 1,218.7 | — |
| `encode_native` | `-` | 12.3 ± 0.2 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 239.0 ± 16.4 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 101.9 ± 1.0 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.0 ± 1.5 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 347.1 ± 3.0 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 167.2 ± 2.1 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 20.9 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 20.9 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.6 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 37.1 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 32.4 ± 0.5 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 26.5 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 13.0 ± 0.2 | — | 48.0 | — |
| `Patch` | `-` | 6.9 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.7 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 22.3 ± 0.3 | — | 160.0 | — |
| `TransformFocus` | `-` | 838.3 ± 6.7 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,105.6 ± 52.1 | — | 6,178.7 | — |
| `TransformFocusPartialFailing` | `-` | 873.1 ± 3.6 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 9.1 ± 0.2 | — | 48.0 | — |
| `TransformImage` | `-` | 23.4 ± 0.2 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 24.7 ± 0.3 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 37.7 ± 0.3 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 29.5 ± 0.5 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.5 ± 0.6 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.8 ± 0.1 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 22.0 ± 0.3 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 825.7 ± 9.7 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 995.4 ± 2.6 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,387.4 ± 15.4 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 27.6 ± 0.2 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.0 ± 0.0 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 20.5 ± 0.6 | — | 144.0 | — |
| `handCodecModify` | `-` | 15.0 ± 0.3 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 482.4 ± 5.4 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 408.4 ± 3.1 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 270.7 ± 0.5 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.9 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 19.6 ± 0.5 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.6 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 598.3 ± 28.0 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 587.3 ± 18.9 | — | 744.0 | — |
| `encode_hand` | `-` | 143.5 ± 1.2 | — | 768.0 | — |
| `encode_positional` | `-` | 6,677.1 ± 128.8 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 8,111.2 ± 90.1 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.1 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 22.0 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 40.9 ± 0.4 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 26.5 ± 0.3 | — | 240.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.6 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 48.3 ± 0.4 | — | 72.0 | — |
| `reuseLens6` | `-` | 134.8 ± 1.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 47.6 ± 0.5 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,369.1 ± 22.7 | 1,348.3 ± 11.9 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 161.0 ± 0.2 | 162.4 ± 2.3 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.8 ± 0.1 | 20.5 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,384.3 ± 81.9 | 2,428.8 ± 62.8 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 154.4 ± 2.1 | 153.8 ± 0.5 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 21.3 ± 0.2 | 21.2 ± 0.2 | 216.0 | 216.0 |

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
| `handLensModify` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.0 ± 0.2 | 9.4 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 31.7 ± 0.0 | 26.7 ± 2.7 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 3.9 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.0 | 3.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 430,598.9 ± 6,910.3 | — | 1,066,770.3 | — |
| `cModifyId` | `size=64` | 55,088.4 ± 933.7 | — | 136,294.4 | — |
| `cModifyId` | `size=8` | 9,065.6 ± 67.8 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 211,832.0 ± 1,205.5 | — | 797,932.2 | — |
| `cReadId` | `size=64` | 27,018.6 ± 310.5 | — | 101,295.5 | — |
| `cReadId` | `size=8` | 5,387.8 ± 1,971.2 | — | 15,568.1 | — |
| `cReadStreet` | `size=512` | 214,120.3 ± 2,620.9 | — | 797,932.9 | — |
| `cReadStreet` | `size=64` | 27,121.5 ± 316.6 | — | 101,295.6 | — |
| `cReadStreet` | `size=8` | 4,157.1 ± 81.8 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 430,023.6 ± 11,827.0 | — | 1,066,709.1 | — |
| `cReplaceId` | `size=64` | 55,273.2 ± 815.7 | — | 136,246.4 | — |
| `cReplaceId` | `size=8` | 9,079.1 ± 66.9 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 346,608.6 ± 2,625.9 | — | 1,240,743.3 | — |
| `cSumPrices` | `size=64` | 42,933.2 ± 491.0 | — | 156,995.4 | — |
| `cSumPrices` | `size=8` | 6,183.6 ± 98.1 | — | 22,682.7 | — |
| `jMiss` | `size=512` | 191.2 ± 4.8 | — | 0.1 | — |
| `jMiss` | `size=64` | 185.4 ± 2.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 188.4 ± 3.5 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,121.1 ± 28.2 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 285.9 ± 7.3 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 104.2 ± 1.6 | — | 984.0 | — |
| `jReadId` | `size=512` | 35.9 ± 1.3 | — | 48.0 | — |
| `jReadId` | `size=64` | 37.4 ± 2.9 | — | 56.0 | — |
| `jReadId` | `size=8` | 37.5 ± 3.1 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 208.4 ± 1.8 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 210.0 ± 2.3 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 208.2 ± 2.1 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,096.8 ± 33.6 | — | 41,888.9 | — |
| `jReplaceId` | `size=64` | 277.7 ± 1.3 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 100.2 ± 2.5 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 89,240.3 ± 403.0 | — | 63,659.2 | — |
| `jSumPrices` | `size=64` | 11,131.7 ± 89.7 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,478.8 ± 19.1 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 84.4 ± 0.7 | — | 320.0 | — |
| `MapDrillModify` | `-` | 41.9 ± 0.5 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 72.6 ± 2.5 | — | 216.0 | — |
| `handEnvUse` | `-` | 84.1 ± 0.6 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 38.1 ± 0.6 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 69.2 ± 2.4 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.4 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 37.0 ± 0.3 | 31.1 ± 0.6 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 16,577.1 ± 869.2 | — | 43,036.3 | — |
| `Fold_powerEach` | `size=256` | 3,543.1 ± 113.6 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 384.2 ± 1.4 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 78.3 ± 0.6 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 56,867.7 ± 2,032.5 | — | 331,130.9 | — |
| `Modify_multiFocus` | `size=256` | 12,938.4 ± 214.6 | — | 76,112.9 | — |
| `Modify_multiFocus` | `size=32` | 1,384.2 ± 10.8 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 206.2 ± 1.4 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 38,036.0 ± 731.7 | — | 115,177.9 | — |
| `Modify_powerEach` | `size=256` | 9,042.6 ± 207.2 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 947.6 ± 136.0 | — | 3,168.0 | — |
| `Modify_powerEach` | `size=4` | 187.2 ± 2.2 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 10,042.0 ± 170.7 | — | 65,578.6 | — |
| `naive_listMap` | `size=256` | 2,521.0 ± 77.9 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 268.4 ± 3.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 34.9 ± 0.4 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,852.2 ± 133.3 | — | 16,129.3 | — |
| `naive_sumQty` | `size=256` | 876.1 ± 44.4 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 64.6 ± 0.6 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 7.2 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 35.7 ± 1.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 133.4 ± 0.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.6 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 27.0 ± 0.1 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 33.3 ± 0.4 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.1 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.0 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 162.2 ± 1.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.4 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,049.4 ± 20.3 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,015.2 ± 15.0 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.5 ± 0.0 | 23.1 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.1 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 47.5 ± 0.3 | 68.4 ± 0.7 | 136.0 | 304.0 |
| `Modify_6` | `-` | 119.4 ± 1.2 | 111.7 ± 0.3 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.4 ± 0.0 | 16.8 ± 0.2 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.5 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.6 ± 0.1 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.7 ± 0.1 | 7.7 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 34,123.6 ± 462.0 | — | 97,424.0 | — |
| `ModifyNames` | `size=64` | 4,315.5 ± 78.0 | — | 12,616.2 | — |
| `ModifyNames` | `size=8` | 570.2 ± 7.6 | — | 1,976.0 | — |
| `ModifyStreet` | `size=512` | 119.9 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 119.1 ± 1.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 119.6 ± 1.8 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 1.1 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.1 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.0 ± 0.5 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 94,077.4 ± 391.4 | — | 382,774.0 | — |
| `monocleModifyNames` | `size=64` | 9,480.7 ± 97.3 | — | 39,856.3 | — |
| `monocleModifyNames` | `size=8` | 1,346.5 ± 6.2 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 47,761.6 ± 209.3 | — | 169,079.5 | — |
| `monocleModifyStreet` | `size=64` | 6,283.2 ± 121.3 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 912.7 ± 24.5 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 26,581.7 ± 121.8 | — | 69,785.0 | — |
| `monocleReadStreet` | `size=64` | 3,438.8 ± 16.0 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 505.6 ± 5.4 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 62,730.0 ± 903.7 | — | 226,297.8 | — |
| `naiveModifyNames` | `size=64` | 8,001.4 ± 82.2 | — | 27,929.2 | — |
| `naiveModifyNames` | `size=8` | 1,060.2 ± 11.9 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 47,707.7 ± 405.7 | — | 169,057.8 | — |
| `naiveModifyStreet` | `size=64` | 6,240.7 ± 46.6 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 903.7 ± 11.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 26,640.4 ± 275.8 | — | 69,784.8 | — |
| `naiveReadStreet` | `size=64` | 3,491.5 ± 116.2 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 502.5 ± 4.3 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 232,487.5 ± 4,021.9 | — | 626,338.3 | — |
| `Names` | `size=64` | 28,558.4 ± 779.2 | — | 78,779.0 | — |
| `Names` | `size=8` | 3,873.9 ± 57.9 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 252,096.8 ± 3,445.0 | — | 695,858.1 | — |
| `NamesIor` | `size=64` | 31,080.7 ± 751.6 | — | 87,490.8 | — |
| `NamesIor` | `size=8` | 4,148.7 ± 155.3 | — | 11,560.1 | — |
| `Street` | `size=512` | 997.0 ± 15.0 | — | 2,720.8 | — |
| `Street` | `size=64` | 990.1 ± 8.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,003.0 ± 25.2 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 990.3 ± 9.3 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,009.1 ± 18.2 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 994.1 ± 13.1 | — | 2,736.0 | — |
| `directNames` | `size=512` | 230,742.6 ± 4,008.3 | — | 609,881.0 | — |
| `directNames` | `size=64` | 28,469.5 ± 582.4 | — | 77,185.7 | — |
| `directNames` | `size=8` | 3,817.6 ± 45.4 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,008.9 ± 22.0 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,010.2 ± 14.8 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 985.8 ± 16.8 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 230,437.5 ± 2,887.0 | — | 613,984.7 | — |
| `hcursorNames` | `size=64` | 27,894.2 ± 110.6 | — | 76,749.0 | — |
| `hcursorNames` | `size=8` | 3,843.0 ± 50.3 | — | 10,781.4 | — |
| `hcursorStreet` | `size=512` | 1,056.3 ± 38.8 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,042.9 ± 13.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,072.7 ± 41.5 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 226,593.0 ± 3,974.5 | — | 1,121,760.4 | — |
| `monocleNames` | `size=64` | 23,861.5 ± 204.3 | — | 132,759.5 | — |
| `monocleNames` | `size=8` | 3,591.5 ± 71.4 | — | 19,466.7 | — |
| `monocleStreet` | `size=512` | 170,820.0 ± 1,736.4 | — | 908,029.5 | — |
| `monocleStreet` | `size=64` | 20,554.0 ± 163.5 | — | 113,809.8 | — |
| `monocleStreet` | `size=8` | 3,076.4 ± 15.9 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 189,745.8 ± 2,648.1 | — | 965,263.5 | — |
| `naiveNames` | `size=64` | 22,283.3 ± 178.5 | — | 120,826.1 | — |
| `naiveNames` | `size=8` | 3,240.0 ± 6.4 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 170,834.7 ± 1,604.6 | — | 902,562.8 | — |
| `naiveStreet` | `size=64` | 20,211.1 ± 692.1 | — | 113,463.1 | — |
| `naiveStreet` | `size=8` | 3,077.1 ± 8.3 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,294.1 ± 33.0 | — | 42,027.0 | — |
| `ModifyStreet` | `size=64` | 489.9 ± 1.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 301.7 ± 1.5 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 212.1 ± 3.1 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 209.2 ± 4.3 | — | 109.4 | — |
| `ReadStreet` | `size=8` | 207.9 ± 5.6 | — | 90.7 | — |
| `SumPrices` | `size=512` | 87,975.6 ± 609.0 | — | 63,726.3 | — |
| `SumPrices` | `size=64` | 11,134.0 ± 118.1 | — | 8,121.1 | — |
| `SumPrices` | `size=8` | 1,462.5 ± 7.2 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 164,421.6 ± 2,958.1 | — | 333,576.7 | — |
| `monocleModifyStreet` | `size=64` | 19,403.9 ± 194.3 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,262.3 ± 35.6 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 93,468.4 ± 998.4 | — | 193,232.0 | — |
| `monocleReadStreet` | `size=64` | 11,468.9 ± 346.7 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,728.1 ± 30.6 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 441,436.4 ± 3,178.2 | — | 1,181,531.6 | — |
| `monocleSumPrices` | `size=64` | 15,585.7 ± 342.1 | — | 45,841.6 | — |
| `monocleSumPrices` | `size=8` | 2,351.9 ± 39.9 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 159,779.0 ± 702.2 | — | 333,565.5 | — |
| `naiveModifyStreet` | `size=64` | 19,785.5 ± 297.2 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,230.4 ± 63.5 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 92,115.6 ± 1,943.4 | — | 193,230.9 | — |
| `naiveReadStreet` | `size=64` | 11,491.8 ± 256.0 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,741.3 ± 37.3 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 96,882.8 ± 1,517.9 | — | 230,123.2 | — |
| `naiveSumPrices` | `size=64` | 12,054.7 ± 347.1 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,831.3 ± 40.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 38,336.5 ± 846.9 | — | 456.1 | — |
| `nativeReadStreet` | `size=64` | 4,949.7 ± 161.0 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 867.2 ± 36.4 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,750.5 ± 340.8 | — | 86,271.6 | — |
| `nativeSumPrices` | `size=64` | 8,078.9 ± 24.5 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,185.6 ± 14.3 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 123,609.4 ± 357.6 | — | 624,386.0 | — |
| `TransformDeep` | `n=512` | 13,085.4 ± 139.9 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,471.7 ± 16.7 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 132,609.4 ± 1,586.4 | 173,302.0 ± 1,878.1 | 655,360.5 | 753,742.1 |
| `TransformExpr` | `n=512` | 16,336.0 ± 149.4 | 16,260.6 ± 209.2 | 81,825.7 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,004.8 ± 13.5 | 2,657.6 ± 29.8 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 118,051.8 ± 4,994.9 | — | 786,589.9 | — |
| `UniverseDeep` | `n=512` | 16,745.1 ± 575.9 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,967.4 ± 14.0 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 105,818.0 ± 3,083.6 | 3,032,585.3 ± 106,735.2 | 786,389.0 | 4,687,882.0 |
| `UniverseExpr` | `n=512` | 15,771.5 ± 213.7 | 199,785.1 ± 12,172.4 | 98,185.6 | 475,012.4 |
| `UniverseExpr` | `n=64` | 1,925.4 ± 28.0 | 17,860.2 ± 963.1 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 230,524.0 ± 2,366.7 | 3,426,491.7 ± 136,784.1 | 786,479.8 | 6,490,033.6 |
| `UniverseJson` | `n=512` | 26,072.1 ± 400.3 | 231,704.8 ± 9,450.0 | 98,186.7 | 699,919.7 |
| `UniverseJson` | `n=64` | 3,019.4 ± 43.9 | 22,084.3 ± 973.2 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 45,535.3 ± 508.4 | — | 163,889.1 | — |
| `visitorTransformDeep` | `n=512` | 4,331.0 ± 183.5 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 442.8 ± 5.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 66,924.1 ± 453.0 | — | 360,472.7 | — |
| `visitorTransformExpr` | `n=512` | 8,369.7 ± 56.2 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,020.6 ± 16.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,079.8 ± 1,116.7 | — | 196,707.0 | — |
| `visitorUniverseDeep` | `n=512` | 7,157.9 ± 67.0 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 797.0 ± 9.8 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 56,682.1 ± 896.1 | — | 196,657.3 | — |
| `visitorUniverseExpr` | `n=512` | 7,049.6 ± 95.2 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 813.9 ± 2.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 95,027.5 ± 646.2 | — | 196,701.2 | — |
| `visitorUniverseJson` | `n=512` | 9,800.1 ± 64.2 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,358.2 ± 11.5 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,513.9 ± 162.9 | — | 41,414.4 | — |
| `Modify_powerEach` | `size=16` | 262.4 ± 1.8 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,361.7 ± 51.6 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 117.2 ± 0.1 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 54,527.2 ± 1,537.1 | — | 164,376.8 | — |
| `Modify_powerEach` | `size=64` | 803.2 ± 9.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 58,677.8 ± 602.1 | — | 279,431.9 | — |
| `monocle_powerEach` | `size=16` | 549.2 ± 1.3 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,556.0 ± 196.9 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 210.8 ± 4.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 187,465.9 ± 8,664.8 | — | 967,857.3 | — |
| `monocle_powerEach` | `size=64` | 2,168.8 ± 53.4 | — | 14,509.4 | — |
| `naive_powerEach` | `size=1024` | 7,082.8 ± 62.3 | — | 28,731.3 | — |
| `naive_powerEach` | `size=16` | 109.1 ± 1.0 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,711.2 ± 15.4 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.7 ± 0.7 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 28,045.2 ± 276.8 | — | 114,789.9 | — |
| `naive_powerEach` | `size=64` | 426.8 ± 5.4 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 69,682.2 ± 4,980.2 | — | 210,646.8 | — |
| `Modify_nested` | `size=16` | 1,420.2 ± 24.5 | — | 4,744.1 | — |
| `Modify_nested` | `size=256` | 18,854.5 ± 194.0 | — | 53,739.8 | — |
| `Modify_nested` | `size=4` | 640.2 ± 5.6 | — | 2,216.0 | — |
| `Modify_nested` | `size=64` | 4,935.0 ± 65.9 | — | 14,592.6 | — |
| `monocle_nested` | `size=1024` | 244,454.4 ± 11,493.6 | — | 1,118,846.4 | — |
| `monocle_nested` | `size=16` | 2,372.6 ± 73.0 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 90,554.2 ± 961.6 | — | 430,208.3 | — |
| `monocle_nested` | `size=4` | 1,122.1 ± 23.8 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 9,539.0 ± 128.7 | — | 58,833.0 | — |
| `naive_nested` | `size=1024` | 24,029.6 ± 130.1 | — | 115,075.8 | — |
| `naive_nested` | `size=16` | 391.8 ± 3.0 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 6,063.8 ± 80.0 | — | 29,019.8 | — |
| `naive_nested` | `size=4` | 139.1 ± 2.8 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,444.5 ± 18.9 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,359.9 ± 15.1 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 24,891.1 ± 386.0 | — | 104,676.0 | — |
| `Modify_sparse` | `size=32` | 357.4 ± 3.1 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 6,227.4 ± 39.2 | — | 24,785.7 | — |
| `Modify_sparse` | `size=8` | 135.3 ± 21.7 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,181.6 ± 42.5 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 98,920.9 ± 1,756.4 | — | 523,142.3 | — |
| `monocle_sparse` | `size=32` | 980.5 ± 13.0 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 32,779.2 ± 385.0 | — | 166,685.5 | — |
| `monocle_sparse` | `size=8` | 303.3 ± 2.3 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 317.9 ± 3.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,280.5 ± 81.7 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 81.2 ± 0.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,302.3 ± 13.8 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.6 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 20.6 ± 0.3 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 35.9 ± 0.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.4 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 98,045.4 ± 4,425.6 | — | 589,712.7 | — |
| `Cata` | `-` | 68,040.2 ± 523.9 | — | 197,568.5 | — |
| `Hylo` | `-` | 88,713.2 ± 1,249.0 | — | 295,848.6 | — |
| `drosteAna` | `-` | 45,638.9 ± 431.4 | — | 327,632.3 | — |
| `drosteCata` | `-` | 41,179.0 ± 3,315.9 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 50,092.8 ± 669.2 | — | 328,640.4 | — |
| `handAna` | `-` | 23,546.2 ± 332.3 | — | 163,816.2 | — |
| `handCata` | `-` | 13,086.6 ± 86.2 | — | 0.1 | — |
| `handHylo` | `-` | 10,221.8 ± 753.3 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.3 ± 0.2 | 27.7 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 24.1 ± 0.4 | 60.6 ± 4.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,631.8 ± 109.3 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 548.2 ± 4.6 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 52.5 ± 4.0 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 2,156.5 ± 66.7 | 30,956.2 ± 419.8 | 12,312.3 | 162,564.9 |
| `FoldPrices` | `size=64` | 153.6 ± 1.2 | 2,106.4 ± 17.7 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 21.3 ± 0.3 | 278.1 ± 1.6 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 8,430.3 ± 381.4 | 33,454.7 ± 427.5 | 36,897.3 | 176,890.9 |
| `Modify` | `size=64` | 886.6 ± 23.2 | 1,831.9 ± 13.6 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 101.5 ± 0.8 | 235.3 ± 1.6 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 11.0 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 97.2 ± 0.6 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.2 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 65.0 ± 0.6 | — | 400.0 | — |
| `handDrillGet` | `-` | 15.3 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 86.7 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 15.4 ± 0.3 | — | 120.0 | — |
| `handServiceReplace` | `-` | 69.7 ± 0.2 | — | 520.0 | — |

