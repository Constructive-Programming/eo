# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `078e36db48e5d7a79435f958d50897c6b7974991` · date: `2026-11-05` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 12.0 ± 0.3 | 7.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 23.3 ± 1.2 | 19.6 ± 0.8 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.8 ± 0.0 | 0.9 ± 0.2 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 120.5 ± 5.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 222.7 ± 2.7 | — | 928.0 | — |
| `ModifyPartner` | `-` | 269.6 ± 5.1 | — | 984.0 | — |
| `ReadCountry` | `-` | 142.7 ± 2.2 | — | 509.3 | — |
| `ReadPartner` | `-` | 160.6 ± 3.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 237.1 ± 4.1 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,056.5 ± 46.8 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,368.1 ± 16.7 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,770.3 ± 22.5 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,269.6 ± 29.7 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,520.8 ± 15.6 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 677.0 ± 2.7 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 442.5 ± 2.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 309.9 ± 0.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 335.3 ± 1.9 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,049.9 ± 7.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,033.6 ± 3.7 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 43.2 ± 0.2 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 496.5 ± 6.4 | — | 568.0 | — |
| `handwritten_stream` | `-` | 116.0 ± 0.6 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 567.9 ± 21.2 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,247.7 ± 109.4 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 1,904.4 ± 10.7 | — | 3,952.0 | — |
| `WideToAvro` | `-` | 558.4 ± 10.3 | — | 2,040.0 | — |
| `WideToJson` | `-` | 453.0 ± 2.3 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,010.7 ± 17.1 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,047.6 ± 16.5 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 703.6 ± 10.6 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,394.2 ± 16.3 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 284.3 ± 21.4 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 247.0 ± 15.0 | — | 693.4 | — |
| `circeStrictFull` | `-` | 277.0 ± 4.0 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 520.9 ± 13.7 | — | 2,528.1 | — |
| `jsoniterDefaultsFull` | `-` | 860.9 ± 10.0 | — | 4,192.0 | — |
| `jsoniterDefaultsSparse` | `-` | 247.6 ± 5.2 | — | 672.0 | — |
| `jsoniterStrictFull` | `-` | 863.6 ± 20.8 | — | 4,192.0 | — |
| `jsoniterStrictSparse` | `-` | 316.6 ± 14.2 | — | 2,888.0 | — |
| `jsoniterWithDefaults` | `-` | 524.9 ± 12.9 | — | 1,016.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 147.2 ± 0.8 | — | 832.0 | — |
| `decode_native` | `-` | 13.2 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 141.7 ± 0.4 | — | 832.0 | — |
| `encode_bridged` | `-` | 175.6 ± 3.1 | — | 1,224.0 | — |
| `encode_native` | `-` | 9.3 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 174.2 ± 0.5 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 80.1 ± 2.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 78.9 ± 2.5 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 271.2 ± 2.1 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 131.3 ± 2.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 16.3 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 16.6 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.2 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.3 ± 0.7 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.3 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 24.0 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 29.6 ± 0.6 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.1 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 25.2 ± 0.1 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 20.7 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 10.6 ± 0.1 | — | 48.0 | — |
| `Patch` | `-` | 5.2 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 12.9 ± 0.2 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 17.4 ± 0.2 | — | 160.0 | — |
| `TransformFocus` | `-` | 707.3 ± 12.0 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 921.7 ± 17.5 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 688.9 ± 1.4 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 7.1 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 17.9 ± 0.4 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 19.1 ± 0.1 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 29.2 ± 0.1 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 23.0 ± 0.1 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 11.0 ± 0.2 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 3.8 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 11.0 ± 0.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 17.0 ± 0.2 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 647.2 ± 6.3 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 798.4 ± 35.3 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,074.9 ± 9.2 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 3.7 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 21.3 ± 0.0 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 11.9 ± 0.0 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 15.9 ± 0.2 | — | 144.0 | — |
| `handCodecModify` | `-` | 11.5 ± 0.2 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 3.7 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.5 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.5 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 370.4 ± 3.4 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 318.8 ± 3.4 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 261.4 ± 1.1 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 3.9 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 15.4 ± 0.9 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 4.3 ± 0.0 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.7 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 472.1 ± 21.4 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 463.4 ± 15.0 | — | 744.0 | — |
| `encode_hand` | `-` | 112.0 ± 2.0 | — | 768.0 | — |
| `encode_positional` | `-` | 5,205.9 ± 89.9 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,311.9 ± 192.8 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 3.9 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 17.1 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 31.7 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 20.6 ± 0.0 | — | 240.0 | — |
| `reuseLeaf` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.9 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 37.4 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 104.6 ± 0.8 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 37.2 ± 0.7 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,089.1 ± 10.4 | 1,088.5 ± 12.3 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 124.9 ± 0.2 | 126.0 ± 3.3 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 16.2 ± 0.0 | 16.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,921.3 ± 33.3 | 1,941.7 ± 36.7 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 119.2 ± 0.3 | 119.7 ± 1.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 16.7 ± 0.3 | 16.5 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.8 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 14.0 ± 0.1 | 7.3 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 24.7 ± 0.1 | 19.5 ± 0.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.8 ± 0.0 | 2.9 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 321,662.9 ± 2,558.0 | — | 1,066,715.5 | — |
| `cModifyId` | `size=64` | 41,658.0 ± 215.2 | — | 136,291.0 | — |
| `cModifyId` | `size=8` | 6,821.4 ± 108.3 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 159,745.3 ± 629.9 | — | 797,917.4 | — |
| `cReadId` | `size=64` | 20,081.6 ± 141.2 | — | 101,288.6 | — |
| `cReadId` | `size=8` | 3,097.8 ± 8.4 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 162,654.9 ± 5,917.6 | — | 797,918.3 | — |
| `cReadStreet` | `size=64` | 20,368.7 ± 49.8 | — | 101,288.6 | — |
| `cReadStreet` | `size=8` | 3,125.6 ± 42.3 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 322,967.1 ± 5,194.2 | — | 1,066,667.9 | — |
| `cReplaceId` | `size=64` | 41,846.6 ± 217.0 | — | 136,242.9 | — |
| `cReplaceId` | `size=8` | 6,768.3 ± 141.0 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 266,370.3 ± 5,220.5 | — | 1,240,715.7 | — |
| `cSumPrices` | `size=64` | 33,151.0 ± 334.2 | — | 157,381.8 | — |
| `cSumPrices` | `size=8` | 4,738.2 ± 49.7 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 144.4 ± 1.5 | — | 0.0 | — |
| `jMiss` | `size=64` | 146.3 ± 3.2 | — | 0.0 | — |
| `jMiss` | `size=8` | 144.1 ± 0.7 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,668.4 ± 7.5 | — | 41,920.8 | — |
| `jModifyId` | `size=64` | 252.6 ± 4.6 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 81.9 ± 0.9 | — | 992.0 | — |
| `jReadId` | `size=512` | 28.0 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 28.3 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=8` | 28.0 ± 0.6 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 158.5 ± 1.0 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 158.1 ± 1.5 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 158.2 ± 0.5 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 2,687.3 ± 34.3 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 239.3 ± 4.2 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 78.1 ± 2.1 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 70,752.4 ± 397.6 | — | 63,671.2 | — |
| `jSumPrices` | `size=64` | 8,814.9 ± 82.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,171.8 ± 14.4 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 65.9 ± 0.3 | — | 320.0 | — |
| `MapDrillModify` | `-` | 31.6 ± 0.7 | — | 216.0 | — |
| `MapGet` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 53.9 ± 0.5 | — | 216.0 | — |
| `handEnvUse` | `-` | 66.0 ± 2.0 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 26.8 ± 0.4 | — | 216.0 | — |
| `handMapGet` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 51.4 ± 1.3 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.1 ± 0.0 | 3.5 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 29.5 ± 0.4 | 23.2 ± 0.0 | 152.0 | 176.0 |
| `Replace` | `-` | 2.7 ± 0.0 | 2.7 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 13,469.8 ± 605.4 | — | 43,035.5 | — |
| `Fold_powerEach` | `size=256` | 2,924.4 ± 111.5 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 300.0 ± 0.7 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 61.2 ± 0.6 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 46,779.0 ± 1,450.1 | — | 331,140.2 | — |
| `Modify_multiFocus` | `size=256` | 10,427.5 ± 73.1 | — | 76,112.6 | — |
| `Modify_multiFocus` | `size=32` | 1,080.8 ± 10.9 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 159.0 ± 1.7 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 31,108.4 ± 689.4 | — | 115,176.1 | — |
| `Modify_powerEach` | `size=256` | 6,681.6 ± 838.4 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 796.8 ± 12.7 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 146.1 ± 3.5 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,168.9 ± 114.7 | — | 65,578.1 | — |
| `naive_listMap` | `size=256` | 2,153.8 ± 138.0 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 215.2 ± 2.7 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 30.9 ± 0.1 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 3,800.8 ± 84.0 | — | 16,129.0 | — |
| `naive_sumQty` | `size=256` | 701.1 ± 45.3 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 50.2 ± 0.6 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 5.6 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 27.4 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 90.0 ± 2.1 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 6.8 ± 0.0 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 21.0 ± 0.1 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 25.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 6.3 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 11.6 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 127.8 ± 1.4 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 37.2 ± 0.5 | — | 184.0 | — |
| `buildAndUse` | `-` | 806.0 ± 11.7 | — | 2,888.0 | — |
| `reuseUse` | `-` | 763.3 ± 14.9 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 16.4 ± 0.1 | 17.6 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.8 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 36.9 ± 0.3 | 52.6 ± 0.2 | 136.0 | 304.0 |
| `Modify_6` | `-` | 92.2 ± 0.3 | 87.0 ± 1.0 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 32.6 ± 0.3 | 11.3 ± 0.0 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 12.8 ± 0.2 | 13.1 ± 0.2 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.5 ± 0.0 | 2.9 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.0 ± 0.1 | 5.9 ± 0.0 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 5.8 ± 0.0 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 26,572.4 ± 709.8 | — | 97,422.2 | — |
| `ModifyNames` | `size=64` | 3,312.7 ± 13.4 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 441.4 ± 6.1 | — | 1,984.0 | — |
| `ModifyStreet` | `size=512` | 93.0 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 93.7 ± 1.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 94.2 ± 1.7 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 26.9 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 26.8 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 26.9 ± 0.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 75,137.9 ± 1,711.4 | — | 382,784.9 | — |
| `monocleModifyNames` | `size=64` | 7,522.8 ± 53.1 | — | 39,856.3 | — |
| `monocleModifyNames` | `size=8` | 1,023.8 ± 19.8 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 36,972.5 ± 907.1 | — | 169,074.0 | — |
| `monocleModifyStreet` | `size=64` | 4,849.2 ± 31.3 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 693.7 ± 11.3 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 20,645.1 ± 123.8 | — | 69,780.9 | — |
| `monocleReadStreet` | `size=64` | 2,677.7 ± 53.7 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 401.9 ± 25.2 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 49,353.8 ± 534.9 | — | 226,290.7 | — |
| `naiveModifyNames` | `size=64` | 6,247.6 ± 141.8 | — | 27,920.2 | — |
| `naiveModifyNames` | `size=8` | 829.3 ± 8.0 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 36,236.0 ± 803.1 | — | 169,049.4 | — |
| `naiveModifyStreet` | `size=64` | 4,865.3 ± 19.8 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 699.7 ± 12.7 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 20,613.2 ± 182.3 | — | 69,780.9 | — |
| `naiveReadStreet` | `size=64` | 2,673.1 ± 24.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 384.5 ± 1.4 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 176,806.4 ± 1,125.7 | — | 626,293.7 | — |
| `Names` | `size=64` | 21,647.7 ± 256.3 | — | 79,298.3 | — |
| `Names` | `size=8` | 3,063.2 ± 83.4 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 184,491.1 ± 3,338.5 | — | 691,699.9 | — |
| `NamesIor` | `size=64` | 23,999.6 ± 1,239.2 | — | 87,493.2 | — |
| `NamesIor` | `size=8` | 3,072.2 ± 67.2 | — | 11,560.1 | — |
| `Street` | `size=512` | 767.7 ± 9.7 | — | 2,720.6 | — |
| `Street` | `size=64` | 754.5 ± 11.6 | — | 2,720.1 | — |
| `Street` | `size=8` | 763.5 ± 13.0 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 767.9 ± 11.2 | — | 2,736.6 | — |
| `StreetIor` | `size=64` | 772.8 ± 13.3 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 760.2 ± 10.2 | — | 2,736.0 | — |
| `directNames` | `size=512` | 176,604.1 ± 2,842.2 | — | 613,941.6 | — |
| `directNames` | `size=64` | 21,455.0 ± 345.9 | — | 77,698.3 | — |
| `directNames` | `size=8` | 2,926.1 ± 33.2 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 768.7 ± 27.5 | — | 2,744.6 | — |
| `directStreet` | `size=64` | 758.7 ± 9.3 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 761.5 ± 13.2 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 173,340.7 ± 4,803.3 | — | 605,730.9 | — |
| `hcursorNames` | `size=64` | 21,456.1 ± 218.7 | — | 77,258.3 | — |
| `hcursorNames` | `size=8` | 2,953.1 ± 67.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 794.0 ± 8.5 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 791.3 ± 12.0 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 808.8 ± 23.5 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 172,610.7 ± 4,350.6 | — | 1,121,724.0 | — |
| `monocleNames` | `size=64` | 18,719.8 ± 423.5 | — | 132,412.3 | — |
| `monocleNames` | `size=8` | 2,840.5 ± 74.1 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 132,451.1 ± 1,231.3 | — | 908,014.3 | — |
| `monocleStreet` | `size=64` | 16,246.2 ± 89.5 | — | 113,814.8 | — |
| `monocleStreet` | `size=8` | 2,446.2 ± 61.2 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 145,640.3 ± 1,799.1 | — | 965,233.9 | — |
| `naiveNames` | `size=64` | 17,629.3 ± 83.1 | — | 120,836.2 | — |
| `naiveNames` | `size=8` | 2,578.2 ± 27.4 | — | 17,808.0 | — |
| `naiveStreet` | `size=512` | 132,209.6 ± 1,082.3 | — | 907,998.2 | — |
| `naiveStreet` | `size=64` | 16,307.8 ± 50.3 | — | 113,806.8 | — |
| `naiveStreet` | `size=8` | 2,436.5 ± 40.0 | — | 17,040.0 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 2,875.9 ± 48.2 | — | 42,026.5 | — |
| `ModifyStreet` | `size=64` | 402.9 ± 3.2 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 231.4 ± 3.3 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 159.2 ± 2.4 | — | 128.1 | — |
| `ReadStreet` | `size=64` | 160.3 ± 1.5 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 159.4 ± 1.7 | — | 128.0 | — |
| `SumPrices` | `size=512` | 72,149.8 ± 553.4 | — | 63,714.4 | — |
| `SumPrices` | `size=64` | 8,909.2 ± 45.8 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,178.6 ± 4.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 127,286.2 ± 3,112.8 | — | 333,563.4 | — |
| `monocleModifyStreet` | `size=64` | 15,292.3 ± 38.6 | — | 30,081.5 | — |
| `monocleModifyStreet` | `size=8` | 2,358.4 ± 20.2 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 72,701.6 ± 227.0 | — | 193,214.2 | — |
| `monocleReadStreet` | `size=64` | 9,081.8 ± 81.6 | — | 24,704.9 | — |
| `monocleReadStreet` | `size=8` | 1,415.1 ± 28.1 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 347,144.1 ± 973.5 | — | 1,178,377.1 | — |
| `monocleSumPrices` | `size=64` | 12,215.8 ± 39.6 | — | 45,817.2 | — |
| `monocleSumPrices` | `size=8` | 1,833.4 ± 12.5 | — | 6,448.0 | — |
| `naiveModifyStreet` | `size=512` | 126,560.7 ± 519.5 | — | 333,504.1 | — |
| `naiveModifyStreet` | `size=64` | 15,293.8 ± 34.6 | — | 30,057.5 | — |
| `naiveModifyStreet` | `size=8` | 2,353.1 ± 32.8 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 72,898.3 ± 509.2 | — | 193,214.4 | — |
| `naiveReadStreet` | `size=64` | 9,056.7 ± 33.0 | — | 24,704.9 | — |
| `naiveReadStreet` | `size=8` | 1,414.2 ± 16.9 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 77,254.8 ± 670.2 | — | 230,106.3 | — |
| `naiveSumPrices` | `size=64` | 9,503.1 ± 19.5 | — | 29,337.0 | — |
| `naiveSumPrices` | `size=8` | 1,471.5 ± 19.0 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 27,830.9 ± 1,544.3 | — | 784.8 | — |
| `nativeReadStreet` | `size=64` | 3,859.1 ± 171.5 | — | 760.4 | — |
| `nativeReadStreet` | `size=8` | 704.7 ± 1.8 | — | 760.0 | — |
| `nativeSumPrices` | `size=512` | 58,517.5 ± 265.8 | — | 147,834.2 | — |
| `nativeSumPrices` | `size=64` | 7,325.7 ± 59.4 | — | 18,720.9 | — |
| `nativeSumPrices` | `size=8` | 1,085.7 ± 12.9 | — | 2,592.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 98,968.2 ± 1,005.2 | — | 472,904.0 | — |
| `TransformDeep` | `n=512` | 6,575.3 ± 152.7 | — | 20,520.7 | — |
| `TransformDeep` | `n=64` | 570.7 ± 3.0 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 72,969.0 ± 126.5 | 134,530.3 ± 1,254.4 | 360,501.1 | 753,713.9 |
| `TransformExpr` | `n=512` | 9,563.2 ± 24.2 | 12,618.9 ± 129.0 | 45,057.0 | 69,585.3 |
| `TransformExpr` | `n=64` | 1,119.1 ± 1.8 | 2,058.6 ± 5.0 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 137,604.3 ± 564.5 | — | 784,292.2 | — |
| `TransformMDeep` | `n=512` | 16,079.2 ± 160.3 | — | 82,249.7 | — |
| `TransformMDeep` | `n=64` | 1,473.6 ± 35.2 | — | 7,160.0 | — |
| `TransformMExpr` | `n=4096` | 143,522.8 ± 1,160.3 | 150,402.8 ± 25,406.7 | 599,557.8 | 849,917.5 |
| `TransformMExpr` | `n=512` | 18,549.0 ± 1,341.8 | 20,495.5 ± 31.2 | 79,319.2 | 104,658.1 |
| `TransformMExpr` | `n=64` | 2,201.7 ± 9.6 | 2,076.0 ± 364.8 | 9,213.4 | 13,392.0 |
| `UniverseDeep` | `n=4096` | 74,979.4 ± 820.1 | — | 360,678.6 | — |
| `UniverseDeep` | `n=512` | 9,255.0 ± 32.7 | — | 45,184.9 | — |
| `UniverseDeep` | `n=64` | 1,526.4 ± 4.6 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 63,847.4 ± 293.6 | 2,346,756.8 ± 152,544.6 | 295,022.5 | 4,687,385.6 |
| `UniverseExpr` | `n=512` | 8,020.5 ± 114.8 | 152,407.5 ± 8,517.5 | 36,928.8 | 475,007.6 |
| `UniverseExpr` | `n=64` | 1,208.7 ± 7.5 | 14,180.8 ± 671.7 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 150,171.2 ± 791.6 | 2,555,095.3 ± 140,343.1 | 393,365.3 | 6,489,401.0 |
| `UniverseJson` | `n=512` | 17,846.1 ± 93.0 | 184,259.5 ± 7,611.3 | 49,193.8 | 699,914.8 |
| `UniverseJson` | `n=64` | 2,222.3 ± 8.0 | 16,897.6 ± 740.6 | 6,152.0 | 73,208.3 |
| `visitorTransformDeep` | `n=4096` | 36,497.4 ± 501.3 | — | 163,882.6 | — |
| `visitorTransformDeep` | `n=512` | 3,653.4 ± 136.0 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 350.0 ± 7.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 52,640.0 ± 561.8 | — | 360,462.3 | — |
| `visitorTransformExpr` | `n=512` | 6,521.3 ± 80.2 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 791.1 ± 3.4 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 67,107.0 ± 945.9 | — | 393,312.8 | — |
| `visitorTransformMDeep` | `n=512` | 6,049.4 ± 28.7 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 485.0 ± 6.8 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 67,385.1 ± 174.8 | — | 447,849.0 | — |
| `visitorTransformMExpr` | `n=512` | 8,336.7 ± 86.4 | — | 50,488.9 | — |
| `visitorTransformMExpr` | `n=64` | 1,026.5 ± 10.9 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 47,459.9 ± 1,176.7 | — | 196,698.5 | — |
| `visitorUniverseDeep` | `n=512` | 5,686.0 ± 21.6 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 617.2 ± 2.8 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 45,146.6 ± 900.6 | — | 196,648.9 | — |
| `visitorUniverseExpr` | `n=512` | 5,485.5 ± 12.1 | — | 24,584.6 | — |
| `visitorUniverseExpr` | `n=64` | 629.7 ± 1.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 73,766.2 ± 985.5 | — | 196,685.7 | — |
| `visitorUniverseJson` | `n=512` | 7,609.7 ± 22.3 | — | 24,600.8 | — |
| `visitorUniverseJson` | `n=64` | 1,047.3 ± 1.5 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 10,750.1 ± 119.4 | — | 41,413.1 | — |
| `Modify_powerEach` | `size=16` | 206.7 ± 3.4 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,711.0 ± 66.8 | — | 10,688.4 | — |
| `Modify_powerEach` | `size=4` | 90.2 ± 0.9 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 42,694.9 ± 1,147.4 | — | 164,357.6 | — |
| `Modify_powerEach` | `size=64` | 626.4 ± 6.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 46,162.0 ± 579.4 | — | 279,423.4 | — |
| `monocle_powerEach` | `size=16` | 427.6 ± 4.4 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 16,961.1 ± 410.7 | — | 107,338.6 | — |
| `monocle_powerEach` | `size=4` | 152.8 ± 9.2 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 143,097.5 ± 3,097.8 | — | 967,785.0 | — |
| `monocle_powerEach` | `size=64` | 1,678.8 ± 62.5 | — | 14,514.7 | — |
| `naive_powerEach` | `size=1024` | 5,622.6 ± 128.7 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 84.6 ± 1.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,336.3 ± 8.9 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 20.7 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,160.8 ± 112.4 | — | 114,780.1 | — |
| `naive_powerEach` | `size=64` | 330.8 ± 5.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 59,195.9 ± 1,129.6 | — | 210,550.4 | — |
| `Modify_nested` | `size=16` | 1,087.7 ± 8.5 | — | 4,744.0 | — |
| `Modify_nested` | `size=256` | 15,583.3 ± 373.5 | — | 53,673.8 | — |
| `Modify_nested` | `size=4` | 492.4 ± 7.8 | — | 2,216.0 | — |
| `Modify_nested` | `size=64` | 4,028.5 ± 94.7 | — | 14,592.5 | — |
| `monocle_nested` | `size=1024` | 191,010.4 ± 632.7 | — | 1,118,747.7 | — |
| `monocle_nested` | `size=16` | 2,030.9 ± 32.8 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 71,348.7 ± 848.2 | — | 430,202.8 | — |
| `monocle_nested` | `size=4` | 986.7 ± 92.7 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 7,936.8 ± 199.1 | — | 58,832.9 | — |
| `naive_nested` | `size=1024` | 20,188.7 ± 481.5 | — | 115,068.8 | — |
| `naive_nested` | `size=16` | 299.3 ± 5.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,088.5 ± 61.7 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 105.6 ± 1.8 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,176.7 ± 12.9 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,055.3 ± 3.4 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 20,376.4 ± 789.7 | — | 104,679.9 | — |
| `Modify_sparse` | `size=32` | 277.9 ± 4.2 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,114.1 ± 132.9 | — | 24,785.4 | — |
| `Modify_sparse` | `size=8` | 93.1 ± 0.2 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,451.2 ± 85.5 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 78,132.4 ± 1,322.5 | — | 523,132.5 | — |
| `monocle_sparse` | `size=32` | 780.4 ± 13.4 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 26,481.2 ± 716.7 | — | 166,680.0 | — |
| `monocle_sparse` | `size=8` | 229.7 ± 3.9 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 245.7 ± 1.0 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,245.8 ± 146.6 | — | 24,611.6 | — |
| `naive_sparse` | `size=32` | 63.4 ± 1.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,048.4 ± 7.2 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 21.7 ± 4.3 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.0 ± 0.0 | 2.1 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.0 ± 0.0 | 2.0 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.0 ± 0.0 | 2.0 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 16.0 ± 0.3 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 27.7 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 5.3 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 8.9 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 73,586.9 ± 224.4 | — | 589,712.5 | — |
| `Cata` | `-` | 46,591.0 ± 175.0 | — | 99,288.3 | — |
| `Hylo` | `-` | 67,655.3 ± 308.5 | — | 295,848.5 | — |
| `drosteAna` | `-` | 35,599.8 ± 119.8 | — | 327,632.3 | — |
| `drosteCata` | `-` | 30,906.0 ± 176.7 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 38,931.1 ± 199.3 | — | 328,640.3 | — |
| `handAna` | `-` | 18,955.4 ± 168.7 | — | 163,816.1 | — |
| `handCata` | `-` | 10,158.2 ± 36.3 | — | 0.1 | — |
| `handHylo` | `-` | 7,939.6 ± 319.8 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.9 ± 0.0 | 1.9 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 10.9 ± 1.9 | 21.7 ± 0.6 | 72.0 | 168.0 |
| `Modify_6` | `-` | 19.4 ± 1.0 | 44.8 ± 0.4 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,595.1 ± 109.2 | — | 20,200.7 | — |
| `FoldNested` | `size=64` | 418.9 ± 0.7 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 41.4 ± 2.5 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,757.8 ± 35.4 | 24,525.3 ± 281.0 | 12,312.3 | 162,563.9 |
| `FoldPrices` | `size=64` | 120.0 ± 2.5 | 1,719.3 ± 17.6 | 1,560.0 | 15,408.0 |
| `FoldPrices` | `size=8` | 16.5 ± 0.2 | 232.2 ± 4.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 6,762.1 ± 164.2 | 26,347.4 ± 121.8 | 36,897.1 | 176,885.6 |
| `Modify` | `size=64` | 657.8 ± 6.3 | 1,523.4 ± 13.1 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 79.3 ± 0.1 | 214.7 ± 2.5 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 8.5 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 71.9 ± 0.6 | — | 472.0 | — |
| `ServiceGet` | `-` | 4.0 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 49.0 ± 0.1 | — | 400.0 | — |
| `handDrillGet` | `-` | 11.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 66.1 ± 0.2 | — | 592.0 | — |
| `handServiceGet` | `-` | 11.6 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 54.0 ± 0.6 | — | 520.0 | — |

