# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `fcadd4cc65a0d5f4dcbddf8574c16c655c45a8d8` · date: `2026-11-23` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 16.5 ± 6.5 | 9.8 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.1 ± 1.2 | 23.7 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 161.2 ± 5.1 | — | 720.0 | — |
| `ModifyCountry` | `-` | 304.5 ± 9.5 | — | 933.3 | — |
| `ModifyPartner` | `-` | 399.3 ± 16.7 | — | 984.0 | — |
| `ReadCountry` | `-` | 176.4 ± 4.0 | — | 520.0 | — |
| `ReadPartner` | `-` | 212.9 ± 5.5 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 315.4 ± 7.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,602.0 ± 31.9 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,948.2 ± 28.3 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,849.4 ± 25.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,627.9 ± 19.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,011.9 ± 16.7 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 936.2 ± 7.8 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 603.6 ± 12.6 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 406.1 ± 3.3 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 438.1 ± 4.5 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,331.7 ± 10.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,285.6 ± 15.2 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 54.2 ± 0.6 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 571.8 ± 35.1 | — | 568.0 | — |
| `handwritten_stream` | `-` | 159.7 ± 2.8 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 619.3 ± 11.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,848.8 ± 23.3 | — | 4,690.7 | — |
| `ClickToJson` | `-` | 2,508.7 ± 23.6 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 849.8 ± 18.3 | — | 2,040.0 | — |
| `WideToJson` | `-` | 689.4 ± 32.1 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,329.7 ± 19.3 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,615.0 ± 20.9 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 942.9 ± 16.0 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,774.1 ± 11.6 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 372.0 ± 11.8 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 314.9 ± 20.0 | — | 696.0 | — |
| `circeStrictFull` | `-` | 377.0 ± 4.1 | — | 1,232.0 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 709.9 ± 6.9 | — | 2,496.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,189.1 ± 7.3 | — | 4,192.0 | — |
| `jsoniterDefaultsSparse` | `-` | 329.2 ± 4.7 | — | 704.0 | — |
| `jsoniterStrictFull` | `-` | 1,184.0 ± 11.7 | — | 4,192.0 | — |
| `jsoniterStrictSparse` | `-` | 477.1 ± 3.6 | — | 2,888.0 | — |
| `jsoniterWithDefaults` | `-` | 714.4 ± 24.1 | — | 1,016.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 192.6 ± 1.9 | — | 832.0 | — |
| `decode_native` | `-` | 17.5 ± 0.5 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 187.5 ± 2.3 | — | 832.0 | — |
| `encode_bridged` | `-` | 253.0 ± 7.4 | — | 1,240.0 | — |
| `encode_native` | `-` | 11.8 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 256.0 ± 8.4 | — | 1,256.0 | — |
| `fieldGet_bridged` | `-` | 100.3 ± 0.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 101.7 ± 0.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 397.6 ± 8.7 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 170.9 ± 5.0 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.1 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.4 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.0 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.2 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.2 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |
| `modifyFCap` | `-` | 8.3 ± 0.1 | — | 56.0 | — |
| `modifyFDerivedHeld` | `-` | 15.4 ± 0.0 | — | 56.0 | — |
| `modifyFDerivedPerCall` | `-` | 15.0 ± 0.2 | — | 56.0 | — |
| `modifyFDirect` | `-` | 8.2 ± 0.3 | — | 56.0 | — |
| `modifyFEachDerivedHeld` | `-` | 119.1 ± 0.9 | — | 432.0 | — |
| `modifyFEachDerivedPerCall` | `-` | 122.4 ± 0.9 | — | 472.0 | — |
| `modifyFEachDirect` | `-` | 120.9 ± 3.2 | — | 448.0 | — |
| `modifyFEachExt` | `-` | 122.2 ± 0.5 | — | 472.0 | — |
| `modifyFExt` | `-` | 14.9 ± 0.1 | — | 56.0 | — |
| `modifyFModifyDirect` | `-` | 5.4 ± 0.0 | — | 40.0 | — |
| `modifyFModifyGeneric` | `-` | 30.5 ± 0.4 | — | 224.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.8 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 25.1 ± 1.1 | — | 160.0 | — |
| `GetterRead` | `-` | 13.5 ± 0.2 | — | 48.0 | — |
| `Patch` | `-` | 6.4 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.2 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 21.1 ± 0.2 | — | 160.0 | — |
| `TransformFocus` | `-` | 842.2 ± 10.2 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,138.4 ± 14.1 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 901.7 ± 7.7 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.6 ± 0.4 | — | 48.0 | — |
| `TransformImage` | `-` | 22.7 ± 0.1 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 23.1 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.4 ± 0.2 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.6 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.7 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.5 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 14.0 ± 0.1 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.4 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 817.2 ± 6.3 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 961.6 ± 4.0 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,368.4 ± 13.7 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.4 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.7 ± 0.3 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.5 ± 0.2 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.9 ± 0.2 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.3 ± 0.1 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 415.4 ± 2.1 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 395.0 ± 5.0 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 273.7 ± 2.5 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.1 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.7 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 530.3 ± 14.4 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 547.7 ± 12.3 | — | 744.0 | — |
| `encode_hand` | `-` | 141.5 ± 1.0 | — | 768.0 | — |
| `encode_positional` | `-` | 6,281.0 ± 48.9 | — | 23,885.4 | — |
| `encode_vulcanFull` | `-` | 7,744.7 ± 40.1 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.6 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 38.6 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.9 ± 0.2 | — | 240.0 | — |
| `kitFusedModify` | `-` | 22.8 ± 0.1 | — | 112.0 | — |
| `kitFusedRead` | `-` | 4.5 ± 0.0 | — | 16.0 | — |
| `kitGenericModify` | `-` | 27.2 ± 0.2 | — | 112.0 | — |
| `kitGenericRead` | `-` | 4.5 ± 0.0 | — | 16.0 | — |
| `kitHandModify` | `-` | 18.2 ± 0.6 | — | 112.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.7 ± 0.4 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.7 ± 1.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.4 ± 0.5 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,245.8 ± 11.7 | 1,242.0 ± 11.6 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 158.1 ± 0.9 | 156.9 ± 0.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.5 ± 0.0 | 20.9 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,160.2 ± 42.3 | 2,142.0 ± 30.0 | 12,312.4 | 12,312.3 |
| `FoldPrices` | `size=64` | 157.1 ± 1.0 | 157.2 ± 1.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.6 ± 0.1 | 26.4 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.4 ± 0.2 | 9.3 ± 1.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.6 ± 0.2 | 24.8 ± 0.6 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.7 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 410,832.7 ± 8,059.2 | — | 1,066,771.7 | — |
| `cModifyId` | `size=64` | 51,133.5 ± 612.1 | — | 136,293.5 | — |
| `cModifyId` | `size=8` | 8,623.5 ± 272.1 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 206,956.2 ± 3,565.4 | — | 797,930.9 | — |
| `cReadId` | `size=64` | 25,809.3 ± 364.9 | — | 101,294.1 | — |
| `cReadId` | `size=8` | 4,063.5 ± 68.2 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 207,831.4 ± 2,630.9 | — | 797,931.1 | — |
| `cReadStreet` | `size=64` | 25,587.7 ± 202.6 | — | 101,294.1 | — |
| `cReadStreet` | `size=8` | 4,125.4 ± 127.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 407,920.3 ± 5,515.1 | — | 1,066,706.1 | — |
| `cReplaceId` | `size=64` | 51,536.2 ± 499.3 | — | 136,245.7 | — |
| `cReplaceId` | `size=8` | 8,555.4 ± 84.9 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 342,785.4 ± 7,707.5 | — | 1,240,739.5 | — |
| `cSumPrices` | `size=64` | 42,735.1 ± 707.2 | — | 157,000.9 | — |
| `cSumPrices` | `size=8` | 6,240.9 ± 54.3 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 175.7 ± 3.3 | — | 0.0 | — |
| `jMiss` | `size=64` | 180.9 ± 3.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 186.5 ± 12.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,911.7 ± 78.4 | — | 41,929.1 | — |
| `jModifyId` | `size=64` | 499.1 ± 13.4 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 116.6 ± 2.8 | — | 984.0 | — |
| `jReadId` | `size=512` | 35.1 ± 1.0 | — | 48.0 | — |
| `jReadId` | `size=64` | 36.3 ± 2.3 | — | 56.0 | — |
| `jReadId` | `size=8` | 34.8 ± 1.4 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 193.7 ± 2.1 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 197.7 ± 6.4 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 192.6 ± 2.0 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,803.7 ± 50.7 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 477.6 ± 11.2 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 113.1 ± 3.4 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 86,331.6 ± 582.0 | — | 63,662.4 | — |
| `jSumPrices` | `size=64` | 11,833.3 ± 372.1 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,621.2 ± 116.1 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 86.8 ± 1.0 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.4 ± 0.5 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 65.9 ± 2.3 | — | 216.0 | — |
| `handEnvUse` | `-` | 85.6 ± 1.5 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.1 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 63.7 ± 3.0 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.3 ± 0.4 | 29.9 ± 0.3 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.1 | 3.5 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,258.7 ± 689.1 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,176.4 ± 60.0 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 377.9 ± 6.5 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 86.4 ± 0.6 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,113.5 ± 2,153.3 | — | 331,117.5 | — |
| `Modify_multiFocus` | `size=256` | 11,723.6 ± 589.5 | — | 76,134.1 | — |
| `Modify_multiFocus` | `size=32` | 1,396.1 ± 22.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 221.4 ± 4.6 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,963.3 ± 585.5 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 7,634.9 ± 1,029.0 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 934.5 ± 112.7 | — | 3,168.0 | — |
| `Modify_powerEach` | `size=4` | 192.9 ± 2.9 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,399.1 ± 42.6 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,087.9 ± 19.5 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 253.1 ± 3.0 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.3 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,113.4 ± 71.3 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 733.6 ± 29.1 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 58.5 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.3 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.6 ± 0.6 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 123.1 ± 0.5 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.9 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.8 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.8 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.4 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 157.6 ± 1.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.5 ± 3.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,145.2 ± 23.3 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,065.9 ± 28.5 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.2 ± 0.1 | 21.7 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 45.2 ± 0.3 | 71.5 ± 0.3 | 136.0 | 304.0 |
| `Modify_6` | `-` | 111.3 ± 1.2 | 128.8 ± 11.3 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 41.3 ± 0.8 | 13.6 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 15.3 ± 0.2 | 15.9 ± 0.7 | 88.0 | 104.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.6 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.6 ± 0.1 | 7.3 ± 0.1 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 8.3 ± 0.1 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,057.1 ± 972.6 | — | 97,423.5 | — |
| `ModifyNames` | `size=64` | 3,983.5 ± 53.5 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 543.8 ± 6.3 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 112.0 ± 2.2 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 111.1 ± 0.9 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.3 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.2 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.2 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.4 ± 0.5 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,877.0 ± 1,105.1 | — | 382,774.9 | — |
| `monocleModifyNames` | `size=64` | 8,934.4 ± 93.9 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,449.3 ± 38.7 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,517.9 ± 361.9 | — | 169,080.1 | — |
| `monocleModifyStreet` | `size=64` | 6,046.7 ± 47.9 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 931.1 ± 13.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 24,985.8 ± 307.4 | — | 69,783.7 | — |
| `monocleReadStreet` | `size=64` | 3,220.7 ± 17.8 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 482.7 ± 5.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,480.7 ± 747.3 | — | 226,296.1 | — |
| `naiveModifyNames` | `size=64` | 7,618.9 ± 37.9 | — | 27,920.3 | — |
| `naiveModifyNames` | `size=8` | 1,094.4 ± 14.1 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,873.7 ± 338.9 | — | 169,056.8 | — |
| `naiveModifyStreet` | `size=64` | 6,038.9 ± 91.9 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 922.3 ± 5.6 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 24,874.5 ± 369.9 | — | 69,783.2 | — |
| `naiveReadStreet` | `size=64` | 3,218.4 ± 17.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 477.1 ± 3.1 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 251,445.2 ± 6,756.5 | — | 626,353.5 | — |
| `Names` | `size=64` | 30,841.0 ± 944.0 | — | 78,779.3 | — |
| `Names` | `size=8` | 4,237.2 ± 109.4 | — | 10,872.1 | — |
| `NamesIor` | `size=512` | 268,550.9 ± 2,872.2 | — | 691,767.2 | — |
| `NamesIor` | `size=64` | 33,694.1 ± 600.7 | — | 86,975.5 | — |
| `NamesIor` | `size=8` | 4,440.7 ± 118.5 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,090.3 ± 51.3 | — | 2,720.9 | — |
| `Street` | `size=64` | 1,077.1 ± 26.4 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,081.1 ± 19.8 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,084.6 ± 33.5 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,077.0 ± 12.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,100.2 ± 48.1 | — | 2,736.0 | — |
| `directNames` | `size=512` | 243,463.5 ± 5,177.6 | — | 609,891.2 | — |
| `directNames` | `size=64` | 30,909.5 ± 1,012.1 | — | 77,708.8 | — |
| `directNames` | `size=8` | 4,300.1 ± 107.7 | — | 10,616.1 | — |
| `directStreet` | `size=512` | 1,026.5 ± 27.1 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,042.0 ± 21.0 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,064.9 ± 15.9 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 247,877.9 ± 4,081.5 | — | 613,998.7 | — |
| `hcursorNames` | `size=64` | 29,636.7 ± 759.4 | — | 77,825.2 | — |
| `hcursorNames` | `size=8` | 4,139.0 ± 32.8 | — | 10,736.1 | — |
| `hcursorStreet` | `size=512` | 1,152.2 ± 32.6 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,127.0 ± 11.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,136.1 ± 10.7 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 230,753.4 ± 3,306.0 | — | 1,121,795.1 | — |
| `monocleNames` | `size=64` | 24,447.8 ± 239.2 | — | 132,776.1 | — |
| `monocleNames` | `size=8` | 3,703.4 ± 35.3 | — | 19,504.1 | — |
| `monocleStreet` | `size=512` | 188,856.3 ± 11,241.4 | — | 902,601.5 | — |
| `monocleStreet` | `size=64` | 21,344.1 ± 124.7 | — | 113,825.9 | — |
| `monocleStreet` | `size=8` | 3,220.2 ± 37.5 | — | 17,080.1 | — |
| `naiveNames` | `size=512` | 201,505.3 ± 6,533.9 | — | 965,303.4 | — |
| `naiveNames` | `size=64` | 23,022.0 ± 445.0 | — | 120,850.8 | — |
| `naiveNames` | `size=8` | 3,426.4 ± 19.3 | — | 17,840.1 | — |
| `naiveStreet` | `size=512` | 187,406.8 ± 6,062.3 | — | 908,077.9 | — |
| `naiveStreet` | `size=64` | 21,432.1 ± 149.4 | — | 113,807.3 | — |
| `naiveStreet` | `size=8` | 3,201.7 ± 28.4 | — | 17,072.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,188.5 ± 66.7 | — | 42,027.7 | — |
| `ModifyStreet` | `size=64` | 695.6 ± 25.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 319.0 ± 19.3 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 192.7 ± 1.8 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 193.0 ± 1.0 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 196.4 ± 2.7 | — | 136.0 | — |
| `SumPrices` | `size=512` | 88,847.5 ± 1,403.5 | — | 63,714.8 | — |
| `SumPrices` | `size=64` | 11,190.4 ± 148.6 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,583.9 ± 92.9 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 165,960.9 ± 1,663.6 | — | 333,580.0 | — |
| `monocleModifyStreet` | `size=64` | 20,036.1 ± 254.8 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,075.5 ± 49.9 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 95,187.6 ± 471.0 | — | 193,233.5 | — |
| `monocleReadStreet` | `size=64` | 11,866.6 ± 50.0 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,821.7 ± 90.2 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 427,009.8 ± 5,752.6 | — | 1,178,447.8 | — |
| `monocleSumPrices` | `size=64` | 15,844.4 ± 95.8 | — | 45,825.6 | — |
| `monocleSumPrices` | `size=8` | 2,394.4 ± 19.3 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 164,516.0 ± 1,554.5 | — | 333,570.8 | — |
| `naiveModifyStreet` | `size=64` | 19,829.4 ± 119.4 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,069.8 ± 27.4 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 95,978.3 ± 1,748.1 | — | 193,234.2 | — |
| `naiveReadStreet` | `size=64` | 11,953.1 ± 77.5 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,781.3 ± 16.5 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 101,080.6 ± 1,971.7 | — | 230,126.6 | — |
| `naiveSumPrices` | `size=64` | 12,539.3 ± 136.2 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,883.5 ± 27.0 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 36,429.2 ± 208.1 | — | 792.5 | — |
| `nativeReadStreet` | `size=64` | 5,232.0 ± 215.4 | — | 760.6 | — |
| `nativeReadStreet` | `size=8` | 982.0 ± 10.4 | — | 760.0 | — |
| `nativeSumPrices` | `size=512` | 74,647.3 ± 832.7 | — | 147,839.6 | — |
| `nativeSumPrices` | `size=64` | 9,404.1 ± 113.9 | — | 18,721.1 | — |
| `nativeSumPrices` | `size=8` | 1,392.2 ± 31.6 | — | 2,592.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EverywhereVarName` | `n=4096` | 113,401.5 ± 1,198.7 | — | 491,613.5 | — |
| `EverywhereVarName` | `n=512` | 15,812.6 ± 171.0 | — | 61,449.6 | — |
| `EverywhereVarName` | `n=64` | 1,757.0 ± 20.2 | — | 7,688.0 | — |
| `EverywhereVarNameGeneric` | `n=4096` | 143,302.4 ± 1,472.2 | — | 568,104.3 | — |
| `EverywhereVarNameGeneric` | `n=512` | 21,522.7 ± 611.3 | — | 76,482.2 | — |
| `EverywhereVarNameGeneric` | `n=64` | 2,200.2 ± 14.3 | — | 8,896.0 | — |
| `TransformDeep` | `n=4096` | 135,286.9 ± 9,778.6 | — | 455,002.5 | — |
| `TransformDeep` | `n=512` | 7,527.8 ± 91.2 | — | 20,520.8 | — |
| `TransformDeep` | `n=64` | 724.6 ± 6.3 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 89,287.8 ± 805.8 | 164,814.8 ± 1,530.4 | 360,513.0 | 753,736.0 |
| `TransformExpr` | `n=512` | 11,709.7 ± 99.1 | 15,357.1 ± 159.3 | 45,057.2 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,370.0 ± 14.5 | 2,500.6 ± 20.6 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 169,717.8 ± 6,215.9 | — | 766,395.5 | — |
| `TransformMDeep` | `n=512` | 18,729.2 ± 136.9 | — | 82,145.9 | — |
| `TransformMDeep` | `n=64` | 1,670.6 ± 193.1 | — | 6,490.7 | — |
| `TransformMExpr` | `n=4096` | 172,300.8 ± 3,174.8 | 192,487.9 ± 14,481.1 | 599,578.7 | 849,948.1 |
| `TransformMExpr` | `n=512` | 22,111.5 ± 1,431.7 | 25,496.0 ± 430.7 | 79,319.6 | 104,658.6 |
| `TransformMExpr` | `n=64` | 2,657.7 ± 22.5 | 3,118.5 ± 31.2 | 9,634.7 | 13,008.1 |
| `UniverseDeep` | `n=4096` | 89,738.1 ± 813.4 | — | 360,689.3 | — |
| `UniverseDeep` | `n=512` | 10,863.6 ± 41.0 | — | 45,185.1 | — |
| `UniverseDeep` | `n=64` | 1,772.4 ± 34.3 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 81,499.7 ± 1,175.3 | 2,975,147.0 ± 200,563.8 | 295,035.3 | 4,840,742.6 |
| `UniverseExpr` | `n=512` | 9,905.0 ± 151.2 | 206,279.0 ± 5,974.3 | 36,929.0 | 475,013.1 |
| `UniverseExpr` | `n=64` | 1,451.6 ± 16.6 | 17,996.0 ± 668.0 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 199,569.4 ± 1,702.5 | 3,254,069.1 ± 165,914.8 | 393,401.3 | 6,489,907.7 |
| `UniverseJson` | `n=512` | 23,045.5 ± 232.9 | 250,152.9 ± 2,525.1 | 49,194.4 | 699,921.7 |
| `UniverseJson` | `n=64` | 2,933.1 ± 22.5 | 22,782.3 ± 727.1 | 6,152.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 43,244.9 ± 1,069.7 | — | 163,887.5 | — |
| `visitorTransformDeep` | `n=512` | 4,955.0 ± 37.2 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 431.8 ± 6.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,458.5 ± 591.5 | — | 360,470.9 | — |
| `visitorTransformExpr` | `n=512` | 7,627.8 ± 47.9 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 995.4 ± 10.2 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 52,537.0 ± 341.5 | — | 229,446.2 | — |
| `visitorTransformMDeep` | `n=512` | 6,284.6 ± 40.1 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 584.6 ± 4.2 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 89,574.6 ± 719.5 | — | 447,865.2 | — |
| `visitorTransformMExpr` | `n=512` | 10,192.5 ± 76.9 | — | 50,489.0 | — |
| `visitorTransformMExpr` | `n=64` | 1,531.2 ± 280.1 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 55,191.9 ± 696.0 | — | 196,704.2 | — |
| `visitorUniverseDeep` | `n=512` | 6,509.9 ± 60.8 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 752.7 ± 5.4 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 53,454.0 ± 524.4 | — | 196,654.9 | — |
| `visitorUniverseExpr` | `n=512` | 6,558.2 ± 53.7 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 813.7 ± 19.6 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 104,460.8 ± 665.6 | — | 196,708.0 | — |
| `visitorUniverseJson` | `n=512` | 9,332.0 ± 118.0 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,355.7 ± 9.9 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,284.8 ± 90.8 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 261.8 ± 2.6 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,014.0 ± 65.7 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 125.0 ± 2.3 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 52,469.3 ± 1,989.0 | — | 164,373.5 | — |
| `Modify_powerEach` | `size=64` | 798.4 ± 6.1 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 61,374.0 ± 1,082.9 | — | 279,433.8 | — |
| `monocle_powerEach` | `size=16` | 611.6 ± 8.5 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 22,075.6 ± 503.9 | — | 107,331.9 | — |
| `monocle_powerEach` | `size=4` | 218.4 ± 13.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 181,795.4 ± 4,453.3 | — | 967,848.0 | — |
| `monocle_powerEach` | `size=64` | 2,197.2 ± 25.7 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,759.9 ± 39.8 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 101.5 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,623.7 ± 6.5 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.9 ± 1.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,950.8 ± 131.4 | — | 114,788.1 | — |
| `naive_powerEach` | `size=64` | 397.9 ± 1.6 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 73,474.4 ± 1,324.0 | — | 210,469.8 | — |
| `Modify_nested` | `size=16` | 1,448.4 ± 17.7 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,344.2 ± 243.8 | — | 53,738.8 | — |
| `Modify_nested` | `size=4` | 682.7 ± 19.7 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,573.8 ± 60.0 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 248,781.9 ± 1,553.6 | — | 1,118,852.9 | — |
| `monocle_nested` | `size=16` | 2,571.0 ± 66.8 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 94,263.6 ± 2,077.3 | — | 430,209.4 | — |
| `monocle_nested` | `size=4` | 1,341.6 ± 109.8 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,996.9 ± 94.2 | — | 58,834.6 | — |
| `naive_nested` | `size=1024` | 21,767.7 ± 161.3 | — | 115,071.6 | — |
| `naive_nested` | `size=16` | 365.3 ± 3.0 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,271.7 ± 24.0 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 126.0 ± 2.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,328.9 ± 11.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,327.4 ± 12.3 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,668.9 ± 907.1 | — | 104,683.2 | — |
| `Modify_sparse` | `size=32` | 345.5 ± 1.7 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,688.8 ± 33.2 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 120.1 ± 0.6 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,910.1 ± 41.0 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 107,025.1 ± 1,288.4 | — | 523,146.7 | — |
| `monocle_sparse` | `size=32` | 1,061.3 ± 16.5 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 33,580.6 ± 351.4 | — | 166,686.1 | — |
| `monocle_sparse` | `size=8` | 345.5 ± 8.3 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 289.6 ± 3.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,961.2 ± 20.1 | — | 24,612.0 | — |
| `naive_sparse` | `size=32` | 74.4 ± 0.6 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,167.4 ± 9.3 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.2 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.5 ± 0.1 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 33.9 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.6 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 94,521.6 ± 920.3 | — | 589,712.7 | — |
| `Cata` | `-` | 60,184.7 ± 854.9 | — | 99,288.4 | — |
| `Hylo` | `-` | 91,650.5 ± 982.0 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,539.0 ± 471.2 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,210.6 ± 335.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,686.3 ± 1,545.9 | — | 328,640.5 | — |
| `handAna` | `-` | 20,593.2 ± 240.3 | — | 163,816.1 | — |
| `handCata` | `-` | 12,652.9 ± 72.8 | — | 0.1 | — |
| `handHylo` | `-` | 9,546.4 ± 52.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.3 ± 0.1 | 28.2 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.7 ± 0.1 | 66.4 ± 1.0 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.1 | 3.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,061.5 ± 91.9 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 533.6 ± 2.7 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 49.1 ± 0.2 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,891.4 ± 48.1 | 31,403.6 ± 404.7 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 156.1 ± 1.1 | 2,074.2 ± 12.4 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 26.0 ± 0.2 | 329.1 ± 3.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,277.3 ± 214.5 | 33,312.4 ± 274.4 | 36,897.2 | 176,890.9 |
| `Modify` | `size=64` | 842.3 ± 4.7 | 1,821.0 ± 22.3 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 95.3 ± 0.6 | 289.6 ± 1.8 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.6 ± 0.3 | — | 0.0 | — |
| `DrillModify` | `-` | 92.3 ± 0.7 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.2 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 64.0 ± 0.6 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 89.0 ± 1.3 | — | 592.0 | — |
| `handServiceGet` | `-` | 13.9 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 77.8 ± 2.5 | — | 520.0 | — |

