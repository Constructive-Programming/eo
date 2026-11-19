# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `0cd34fdd4f55536509fd51bddb3631e444f15f61` · date: `2026-11-19` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.7 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 11.4 ± 0.1 | 7.2 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 21.7 ± 0.1 | 19.6 ± 1.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 121.8 ± 1.4 | — | 720.0 | — |
| `ModifyCountry` | `-` | 270.6 ± 2.9 | — | 928.0 | — |
| `ModifyPartner` | `-` | 319.7 ± 4.3 | — | 984.0 | — |
| `ReadCountry` | `-` | 150.6 ± 3.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 168.8 ± 2.3 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 242.4 ± 1.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,390.4 ± 65.5 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,878.6 ± 45.2 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,764.3 ± 98.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,449.5 ± 18.6 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,841.5 ± 37.0 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 866.4 ± 7.5 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 620.7 ± 13.1 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 310.4 ± 1.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 344.9 ± 1.8 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,304.5 ± 17.9 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,246.5 ± 9.6 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.0 ± 0.7 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 502.2 ± 5.4 | — | 568.0 | — |
| `handwritten_stream` | `-` | 165.0 ± 7.2 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 733.4 ± 4.5 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,629.0 ± 11.7 | — | 4,632.0 | — |
| `ClickToJson` | `-` | 2,437.5 ± 28.3 | — | 3,994.7 | — |
| `WideToAvro` | `-` | 743.4 ± 20.4 | — | 2,050.7 | — |
| `WideToJson` | `-` | 524.2 ± 8.4 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,431.4 ± 10.6 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,701.2 ± 56.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 1,060.1 ± 7.2 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,700.7 ± 107.6 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 289.2 ± 6.0 | — | 1,210.7 | — |
| `circeDefaultsSparse` | `-` | 270.1 ± 6.3 | — | 856.0 | — |
| `circeStrictFull` | `-` | 290.0 ± 4.4 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 2.9 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 651.8 ± 17.2 | — | 2,440.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,227.0 ± 5.9 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 274.2 ± 11.6 | — | 672.0 | — |
| `jsoniterStrictFull` | `-` | 1,224.7 ± 5.3 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 575.4 ± 10.4 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 606.5 ± 7.8 | — | 1,048.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 157.1 ± 1.5 | — | 832.0 | — |
| `decode_native` | `-` | 13.1 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 154.7 ± 0.9 | — | 832.0 | — |
| `encode_bridged` | `-` | 198.9 ± 3.0 | — | 1,224.0 | — |
| `encode_native` | `-` | 10.9 ± 0.4 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 196.9 ± 2.9 | — | 1,229.3 | — |
| `fieldGet_bridged` | `-` | 83.9 ± 0.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 84.3 ± 0.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 313.3 ± 6.6 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 137.3 ± 2.0 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 19.3 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 20.6 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 19.4 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.2 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.6 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.4 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 28.6 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.9 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.7 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.7 ± 0.1 | — | 40.0 | — |
| `modifyFCap` | `-` | 9.4 ± 0.1 | — | 56.0 | — |
| `modifyFDerivedHeld` | `-` | 12.6 ± 0.1 | — | 56.0 | — |
| `modifyFDerivedPerCall` | `-` | 12.0 ± 0.1 | — | 56.0 | — |
| `modifyFDirect` | `-` | 9.2 ± 0.0 | — | 56.0 | — |
| `modifyFEachDerivedHeld` | `-` | 98.1 ± 0.5 | — | 432.0 | — |
| `modifyFEachDerivedPerCall` | `-` | 99.9 ± 0.7 | — | 472.0 | — |
| `modifyFEachDirect` | `-` | 98.2 ± 0.5 | — | 448.0 | — |
| `modifyFEachExt` | `-` | 100.1 ± 0.6 | — | 472.0 | — |
| `modifyFExt` | `-` | 12.0 ± 0.1 | — | 56.0 | — |
| `modifyFModifyDirect` | `-` | 7.0 ± 0.0 | — | 40.0 | — |
| `modifyFModifyGeneric` | `-` | 34.4 ± 0.3 | — | 224.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 26.1 ± 0.3 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 27.7 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 10.7 ± 0.2 | — | 48.0 | — |
| `Patch` | `-` | 7.4 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.5 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 26.2 ± 0.1 | — | 160.0 | — |
| `TransformFocus` | `-` | 996.2 ± 6.6 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,192.1 ± 17.7 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 1,166.9 ± 12.1 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.7 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 18.5 ± 0.2 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 18.5 ± 0.3 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 33.4 ± 0.2 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 24.0 ± 0.1 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 13.1 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 6.7 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 12.8 ± 0.2 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 25.6 ± 0.2 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 989.1 ± 14.1 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 1,061.5 ± 6.9 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,674.9 ± 14.4 | — | 10,578.7 | — |
| `chimneyTransformFocusWrite` | `-` | 6.6 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 25.6 ± 0.1 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.5 ± 0.1 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 23.4 ± 0.5 | — | 144.0 | — |
| `handCodecModify` | `-` | 13.3 ± 0.1 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 6.7 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 2.4 ± 0.1 | — | 16.0 | — |
| `handTransformFocus` | `-` | 522.2 ± 1.6 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 694.3 ± 2.3 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 473.4 ± 2.3 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 6.9 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 17.1 ± 0.1 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 8.7 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.8 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 460.1 ± 8.6 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 447.6 ± 4.8 | — | 744.0 | — |
| `encode_hand` | `-` | 127.3 ± 1.1 | — | 768.0 | — |
| `encode_positional` | `-` | 4,769.6 ± 43.3 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 5,689.3 ± 37.6 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 10.3 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 28.1 ± 0.4 | — | 184.0 | — |
| `buildLens6` | `-` | 53.3 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 35.6 ± 0.1 | — | 240.0 | — |
| `kitFusedModify` | `-` | 20.7 ± 0.1 | — | 112.0 | — |
| `kitFusedRead` | `-` | 3.8 ± 0.0 | — | 16.0 | — |
| `kitGenericModify` | `-` | 23.8 ± 0.1 | — | 112.0 | — |
| `kitGenericRead` | `-` | 3.9 ± 0.0 | — | 16.0 | — |
| `kitHandModify` | `-` | 18.6 ± 0.1 | — | 112.0 | — |
| `reuseLeaf` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 12.0 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 33.3 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 91.7 ± 0.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 35.6 ± 0.2 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,432.7 ± 40.1 | 1,409.4 ± 15.3 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 153.0 ± 0.7 | 152.5 ± 0.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 19.4 ± 0.1 | 19.2 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,627.9 ± 38.2 | 2,631.3 ± 35.5 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 221.9 ± 0.8 | 221.8 ± 0.8 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 30.1 ± 0.2 | 30.1 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.1 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.5 ± 0.3 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.4 ± 0.1 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.8 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 12.0 ± 0.3 | 6.2 ± 0.4 | 0.0 | 0.0 |
| `Get_6` | `-` | 22.9 ± 0.1 | 19.8 ± 1.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.8 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.7 ± 0.1 | 4.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.5 ± 0.0 | 4.6 ± 0.2 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 330,617.8 ± 5,503.5 | — | 1,066,715.2 | — |
| `cModifyId` | `size=64` | 42,547.3 ± 555.2 | — | 136,291.4 | — |
| `cModifyId` | `size=8` | 7,208.5 ± 114.4 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 173,047.4 ± 1,632.4 | — | 797,921.2 | — |
| `cReadId` | `size=64` | 22,305.6 ± 211.1 | — | 101,289.1 | — |
| `cReadId` | `size=8` | 3,451.4 ± 56.0 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 176,896.8 ± 2,470.2 | — | 797,922.3 | — |
| `cReadStreet` | `size=64` | 22,405.3 ± 226.2 | — | 101,289.1 | — |
| `cReadStreet` | `size=8` | 3,479.8 ± 64.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 322,745.3 ± 2,499.2 | — | 1,066,667.9 | — |
| `cReplaceId` | `size=64` | 45,431.4 ± 9,272.7 | — | 136,243.4 | — |
| `cReplaceId` | `size=8` | 7,128.8 ± 56.8 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 282,418.6 ± 3,085.4 | — | 1,240,720.3 | — |
| `cSumPrices` | `size=64` | 35,124.1 ± 499.5 | — | 157,282.0 | — |
| `cSumPrices` | `size=8` | 5,353.3 ± 44.9 | — | 22,717.3 | — |
| `jMiss` | `size=512` | 167.6 ± 3.2 | — | 0.0 | — |
| `jMiss` | `size=64` | 168.5 ± 3.1 | — | 0.0 | — |
| `jMiss` | `size=8` | 165.4 ± 2.2 | — | 0.0 | — |
| `jModifyId` | `size=512` | 6,001.2 ± 59.0 | — | 41,929.7 | — |
| `jModifyId` | `size=64` | 763.9 ± 5.8 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 156.7 ± 3.5 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.2 ± 5.5 | — | 64.0 | — |
| `jReadId` | `size=64` | 39.0 ± 4.6 | — | 72.0 | — |
| `jReadId` | `size=8` | 33.7 ± 6.0 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 173.9 ± 1.7 | — | 136.0 | — |
| `jReadStreet` | `size=64` | 173.3 ± 2.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 172.2 ± 2.0 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 6,017.7 ± 84.4 | — | 41,889.7 | — |
| `jReplaceId` | `size=64` | 761.7 ± 3.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 154.5 ± 3.1 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 80,404.2 ± 891.8 | — | 63,667.5 | — |
| `jSumPrices` | `size=64` | 10,327.8 ± 89.3 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,356.3 ± 30.6 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 98.3 ± 0.5 | — | 320.0 | — |
| `MapDrillModify` | `-` | 38.1 ± 0.8 | — | 216.0 | — |
| `MapGet` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 83.9 ± 0.4 | — | 216.0 | — |
| `handEnvUse` | `-` | 96.4 ± 0.6 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 35.5 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 78.8 ± 3.8 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.7 ± 0.1 | 5.7 ± 0.2 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 28.8 ± 0.3 | 29.2 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 5.5 ± 0.1 | 5.4 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 16,759.8 ± 1,281.5 | — | 43,036.3 | — |
| `Fold_powerEach` | `size=256` | 3,025.7 ± 27.5 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 326.6 ± 1.8 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 63.3 ± 0.3 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 60,707.9 ± 1,470.9 | — | 331,134.8 | — |
| `Modify_multiFocus` | `size=256` | 13,597.3 ± 160.2 | — | 76,112.9 | — |
| `Modify_multiFocus` | `size=32` | 1,623.3 ± 69.7 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 204.0 ± 1.4 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 33,034.4 ± 782.7 | — | 115,176.6 | — |
| `Modify_powerEach` | `size=256` | 7,259.3 ± 82.6 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 864.3 ± 5.7 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 148.8 ± 2.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 10,763.7 ± 105.9 | — | 65,578.8 | — |
| `naive_listMap` | `size=256` | 2,644.9 ± 9.7 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 331.9 ± 1.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 47.0 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 5,532.4 ± 198.3 | — | 16,129.4 | — |
| `naive_sumQty` | `size=256` | 673.8 ± 3.0 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 61.1 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 6.6 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 29.3 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 135.6 ± 0.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 13.4 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 36.2 ± 0.3 | — | 248.0 | — |
| `naive_constSum` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 26.6 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 13.1 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 24.9 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 229.8 ± 5.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 42.4 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 918.5 ± 10.9 | — | 2,888.0 | — |
| `reuseUse` | `-` | 875.4 ± 21.5 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 16.7 ± 0.1 | 19.9 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.1 | 0.0 | 0.0 |
| `Modify_3` | `-` | 35.7 ± 0.3 | 57.4 ± 0.8 | 136.0 | 304.0 |
| `Modify_6` | `-` | 87.2 ± 0.9 | 94.3 ± 1.6 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 34.7 ± 0.5 | 16.3 ± 0.0 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 14.9 ± 0.5 | 18.3 ± 0.3 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.6 ± 0.1 | 0.0 | 0.0 |
| `Replace_0` | `-` | 5.8 ± 0.1 | 5.5 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 12.0 ± 0.0 | 11.9 ± 0.1 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 14.3 ± 0.2 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 29,494.7 ± 240.2 | — | 97,422.9 | — |
| `ModifyNames` | `size=64` | 3,490.3 ± 45.6 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 498.3 ± 5.7 | — | 1,976.0 | — |
| `ModifyStreet` | `size=512` | 101.6 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 101.4 ± 2.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 101.2 ± 0.8 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 29.6 ± 3.1 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 29.1 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 28.9 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 80,295.1 ± 833.7 | — | 382,782.0 | — |
| `monocleModifyNames` | `size=64` | 8,213.3 ± 58.5 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,214.1 ± 7.8 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 38,290.0 ± 418.7 | — | 169,075.1 | — |
| `monocleModifyStreet` | `size=64` | 5,014.6 ± 29.0 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 774.3 ± 12.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 19,884.4 ± 518.2 | — | 69,780.8 | — |
| `monocleReadStreet` | `size=64` | 2,519.6 ± 13.1 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 387.1 ± 6.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 48,922.9 ± 137.1 | — | 226,290.4 | — |
| `naiveModifyNames` | `size=64` | 6,306.8 ± 55.3 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 948.0 ± 10.7 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 38,205.2 ± 214.2 | — | 169,051.0 | — |
| `naiveModifyStreet` | `size=64` | 4,970.2 ± 22.3 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 769.9 ± 11.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 19,693.9 ± 103.5 | — | 69,780.7 | — |
| `naiveReadStreet` | `size=64` | 2,530.1 ± 78.7 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 377.7 ± 3.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 188,691.5 ± 6,161.2 | — | 622,199.2 | — |
| `Names` | `size=64` | 24,059.4 ± 680.2 | — | 77,738.5 | — |
| `Names` | `size=8` | 3,448.7 ± 60.9 | — | 10,872.1 | — |
| `NamesIor` | `size=512` | 211,418.4 ± 3,827.3 | — | 691,721.5 | — |
| `NamesIor` | `size=64` | 26,852.8 ± 361.6 | — | 86,969.0 | — |
| `NamesIor` | `size=8` | 3,635.8 ± 44.7 | — | 11,560.1 | — |
| `Street` | `size=512` | 861.4 ± 8.5 | — | 2,720.7 | — |
| `Street` | `size=64` | 879.6 ± 18.2 | — | 2,720.1 | — |
| `Street` | `size=8` | 864.9 ± 12.3 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 871.3 ± 26.0 | — | 2,736.7 | — |
| `StreetIor` | `size=64` | 858.9 ± 22.3 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 861.9 ± 21.2 | — | 2,736.0 | — |
| `directNames` | `size=512` | 189,703.8 ± 4,764.5 | — | 609,848.1 | — |
| `directNames` | `size=64` | 24,252.3 ± 487.1 | — | 77,179.4 | — |
| `directNames` | `size=8` | 3,252.8 ± 57.5 | — | 10,616.1 | — |
| `directStreet` | `size=512` | 857.9 ± 6.8 | — | 2,736.7 | — |
| `directStreet` | `size=64` | 871.0 ± 11.4 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 857.5 ± 12.5 | — | 2,744.0 | — |
| `hcursorNames` | `size=512` | 184,991.4 ± 8,256.3 | — | 605,740.3 | — |
| `hcursorNames` | `size=64` | 24,616.6 ± 862.5 | — | 77,780.7 | — |
| `hcursorNames` | `size=8` | 3,254.1 ± 71.1 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 924.4 ± 12.2 | — | 3,032.7 | — |
| `hcursorStreet` | `size=64` | 932.4 ± 7.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 931.2 ± 10.3 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 194,821.7 ± 1,235.1 | — | 1,121,738.9 | — |
| `monocleNames` | `size=64` | 22,174.6 ± 313.6 | — | 132,413.3 | — |
| `monocleNames` | `size=8` | 3,502.0 ± 130.9 | — | 19,482.7 | — |
| `monocleStreet` | `size=512` | 150,266.6 ± 974.5 | — | 908,005.0 | — |
| `monocleStreet` | `size=64` | 18,990.2 ± 110.8 | — | 113,820.4 | — |
| `monocleStreet` | `size=8` | 2,982.3 ± 10.2 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 162,445.2 ± 618.9 | — | 965,245.2 | — |
| `naiveNames` | `size=64` | 20,346.8 ± 128.0 | — | 120,857.8 | — |
| `naiveNames` | `size=8` | 3,132.5 ± 35.9 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 150,166.4 ± 770.6 | — | 908,010.2 | — |
| `naiveStreet` | `size=64` | 19,022.5 ± 167.6 | — | 113,780.4 | — |
| `naiveStreet` | `size=8` | 2,994.0 ± 22.0 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 6,137.7 ± 123.3 | — | 42,029.5 | — |
| `ModifyStreet` | `size=64` | 890.7 ± 3.2 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 268.4 ± 3.5 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 171.3 ± 1.8 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 172.1 ± 3.9 | — | 109.4 | — |
| `ReadStreet` | `size=8` | 170.4 ± 3.8 | — | 90.7 | — |
| `SumPrices` | `size=512` | 79,030.9 ± 538.1 | — | 63,714.7 | — |
| `SumPrices` | `size=64` | 10,082.1 ± 140.8 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,352.3 ± 43.9 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 139,306.6 ± 4,442.5 | — | 333,555.8 | — |
| `monocleModifyStreet` | `size=64` | 16,774.6 ± 307.9 | — | 30,081.7 | — |
| `monocleModifyStreet` | `size=8` | 2,909.4 ± 29.6 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 73,768.1 ± 739.5 | — | 193,215.1 | — |
| `monocleReadStreet` | `size=64` | 9,260.5 ± 50.5 | — | 24,704.9 | — |
| `monocleReadStreet` | `size=8` | 1,494.5 ± 13.0 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 338,343.1 ± 3,897.1 | — | 1,178,369.6 | — |
| `monocleSumPrices` | `size=64` | 13,305.8 ± 128.1 | — | 45,825.4 | — |
| `monocleSumPrices` | `size=8` | 2,156.5 ± 20.7 | — | 6,333.4 | — |
| `naiveModifyStreet` | `size=512` | 141,760.4 ± 984.2 | — | 333,515.2 | — |
| `naiveModifyStreet` | `size=64` | 17,232.8 ± 147.4 | — | 30,057.7 | — |
| `naiveModifyStreet` | `size=8` | 2,998.6 ± 65.4 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 75,466.4 ± 758.0 | — | 193,216.6 | — |
| `naiveReadStreet` | `size=64` | 9,523.5 ± 61.0 | — | 24,705.0 | — |
| `naiveReadStreet` | `size=8` | 1,536.8 ± 16.0 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 81,269.8 ± 866.5 | — | 230,109.6 | — |
| `naiveSumPrices` | `size=64` | 10,204.1 ± 110.3 | — | 29,337.0 | — |
| `naiveSumPrices` | `size=8` | 1,626.1 ± 16.9 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 34,516.6 ± 352.7 | — | 452.9 | — |
| `nativeReadStreet` | `size=64` | 4,458.2 ± 71.1 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 795.8 ± 5.6 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 55,921.1 ± 757.7 | — | 86,271.1 | — |
| `nativeSumPrices` | `size=64` | 6,823.4 ± 46.4 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 1,025.9 ± 21.7 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EverywhereVarName` | `n=4096` | 103,490.7 ± 606.6 | — | 491,609.6 | — |
| `EverywhereVarName` | `n=512` | 13,338.4 ± 133.5 | — | 61,449.4 | — |
| `EverywhereVarName` | `n=64` | 1,614.4 ± 16.4 | — | 7,688.0 | — |
| `EverywhereVarNameGeneric` | `n=4096` | 125,801.6 ± 593.9 | — | 568,091.6 | — |
| `EverywhereVarNameGeneric` | `n=512` | 18,226.5 ± 156.0 | — | 76,481.9 | — |
| `EverywhereVarNameGeneric` | `n=64` | 1,932.4 ± 17.1 | — | 8,896.0 | — |
| `TransformDeep` | `n=4096` | 122,176.1 ± 891.6 | — | 454,992.9 | — |
| `TransformDeep` | `n=512` | 6,890.9 ± 152.6 | — | 20,520.7 | — |
| `TransformDeep` | `n=64` | 665.8 ± 6.5 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 80,253.2 ± 532.0 | 152,648.7 ± 1,175.6 | 360,506.4 | 753,727.1 |
| `TransformExpr` | `n=512` | 10,235.8 ± 121.0 | 14,001.7 ± 253.2 | 45,057.0 | 69,585.4 |
| `TransformExpr` | `n=64` | 1,235.2 ± 15.9 | 2,352.6 ± 12.4 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 156,422.0 ± 1,111.3 | — | 764,361.8 | — |
| `TransformMDeep` | `n=512` | 17,442.4 ± 108.9 | — | 82,145.8 | — |
| `TransformMDeep` | `n=64` | 1,509.4 ± 159.0 | — | 6,490.7 | — |
| `TransformMExpr` | `n=4096` | 144,356.7 ± 4,158.2 | 203,939.8 ± 56,070.9 | 566,465.1 | 849,956.4 |
| `TransformMExpr` | `n=512` | 18,885.7 ± 1,461.4 | 26,140.7 ± 6,180.3 | 78,559.3 | 104,658.7 |
| `TransformMExpr` | `n=64` | 2,368.2 ± 174.0 | 2,459.6 ± 257.7 | 9,213.4 | 12,328.0 |
| `UniverseDeep` | `n=4096` | 92,079.8 ± 910.9 | — | 360,691.0 | — |
| `UniverseDeep` | `n=512` | 11,381.3 ± 198.2 | — | 45,185.2 | — |
| `UniverseDeep` | `n=64` | 1,579.1 ± 16.7 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 84,609.7 ± 1,005.2 | 5,145,386.9 ± 250,548.4 | 295,037.6 | 4,689,412.6 |
| `UniverseExpr` | `n=512` | 10,172.6 ± 53.0 | 359,650.9 ± 2,778.5 | 36,929.0 | 475,028.7 |
| `UniverseExpr` | `n=64` | 1,301.3 ± 13.5 | 31,563.7 ± 215.8 | 4,640.0 | 45,424.6 |
| `UniverseJson` | `n=4096` | 183,180.8 ± 1,492.6 | 5,501,682.6 ± 211,137.8 | 393,389.3 | 6,491,535.7 |
| `UniverseJson` | `n=512` | 22,459.0 ± 167.3 | 393,880.1 ± 2,270.7 | 49,194.3 | 699,936.2 |
| `UniverseJson` | `n=64` | 2,458.7 ± 14.3 | 35,804.1 ± 433.5 | 6,152.0 | 73,208.7 |
| `visitorTransformDeep` | `n=4096` | 44,851.4 ± 281.3 | — | 163,888.6 | — |
| `visitorTransformDeep` | `n=512` | 4,774.6 ± 27.9 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 531.4 ± 3.0 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 68,906.2 ± 715.5 | — | 360,474.2 | — |
| `visitorTransformExpr` | `n=512` | 8,085.6 ± 89.4 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,048.6 ± 11.4 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 53,654.1 ± 393.7 | — | 229,447.1 | — |
| `visitorTransformMDeep` | `n=512` | 6,076.3 ± 18.8 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 663.7 ± 10.2 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 82,889.1 ± 1,328.0 | — | 447,860.3 | — |
| `visitorTransformMExpr` | `n=512` | 9,449.2 ± 110.7 | — | 50,489.0 | — |
| `visitorTransformMExpr` | `n=64` | 1,268.7 ± 6.7 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 61,595.1 ± 389.1 | — | 196,708.8 | — |
| `visitorUniverseDeep` | `n=512` | 7,473.7 ± 46.7 | — | 24,632.8 | — |
| `visitorUniverseDeep` | `n=64` | 773.2 ± 24.5 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 53,945.0 ± 423.9 | — | 196,655.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,621.4 ± 27.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 784.2 ± 8.9 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 87,564.3 ± 1,152.4 | — | 196,695.7 | — |
| `visitorUniverseJson` | `n=512` | 9,279.2 ± 76.6 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,263.4 ± 10.3 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,006.8 ± 76.9 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 209.7 ± 1.6 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,816.6 ± 22.9 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 102.1 ± 1.1 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 48,972.5 ± 1,358.4 | — | 164,367.7 | — |
| `Modify_powerEach` | `size=64` | 750.4 ± 6.8 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 56,064.5 ± 748.1 | — | 279,430.1 | — |
| `monocle_powerEach` | `size=16` | 649.3 ± 6.8 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 19,859.9 ± 169.4 | — | 107,339.1 | — |
| `monocle_powerEach` | `size=4` | 181.2 ± 5.8 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 190,562.1 ± 3,136.1 | — | 967,862.3 | — |
| `monocle_powerEach` | `size=64` | 2,771.1 ± 1,237.1 | — | 14,514.8 | — |
| `naive_powerEach` | `size=1024` | 5,890.4 ± 63.9 | — | 28,730.8 | — |
| `naive_powerEach` | `size=16` | 93.1 ± 0.8 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,378.6 ± 15.1 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 28.3 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,077.2 ± 238.2 | — | 114,781.7 | — |
| `naive_powerEach` | `size=64` | 353.7 ± 11.0 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 58,673.9 ± 1,406.3 | — | 210,573.5 | — |
| `Modify_nested` | `size=16` | 1,255.0 ± 32.3 | — | 4,768.0 | — |
| `Modify_nested` | `size=256` | 16,796.1 ± 232.0 | — | 53,759.9 | — |
| `Modify_nested` | `size=4` | 604.1 ± 13.9 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,264.3 ± 84.9 | — | 14,693.8 | — |
| `monocle_nested` | `size=1024` | 226,608.9 ± 3,156.9 | — | 1,118,812.5 | — |
| `monocle_nested` | `size=16` | 2,793.0 ± 35.5 | — | 15,749.4 | — |
| `monocle_nested` | `size=256` | 83,787.8 ± 1,630.1 | — | 430,206.8 | — |
| `monocle_nested` | `size=4` | 1,064.7 ± 11.5 | — | 5,546.7 | — |
| `monocle_nested` | `size=64` | 9,824.1 ± 247.1 | — | 58,833.1 | — |
| `naive_nested` | `size=1024` | 21,347.6 ± 81.4 | — | 115,070.9 | — |
| `naive_nested` | `size=16` | 377.7 ± 3.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,243.7 ± 40.9 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 134.9 ± 1.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,332.8 ± 6.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,268.8 ± 28.5 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 21,938.3 ± 558.4 | — | 104,681.6 | — |
| `Modify_sparse` | `size=32` | 321.2 ± 2.4 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,375.6 ± 42.3 | — | 24,785.5 | — |
| `Modify_sparse` | `size=8` | 104.4 ± 0.7 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,614.7 ± 51.4 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 101,620.5 ± 885.0 | — | 523,144.9 | — |
| `monocle_sparse` | `size=32` | 1,193.2 ± 21.0 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 31,981.1 ± 551.6 | — | 166,684.2 | — |
| `monocle_sparse` | `size=8` | 369.9 ± 15.6 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 351.9 ± 8.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,290.6 ± 27.6 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 91.2 ± 1.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,279.6 ± 6.2 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 23.5 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.7 ± 0.0 | 2.8 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.7 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.7 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 15.0 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 26.2 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 17.3 ± 0.2 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 106,107.3 ± 2,492.4 | — | 589,712.8 | — |
| `Cata` | `-` | 49,414.2 ± 375.0 | — | 99,288.4 | — |
| `Hylo` | `-` | 82,299.5 ± 999.8 | — | 295,848.6 | — |
| `drosteAna` | `-` | 57,344.2 ± 877.7 | — | 327,632.4 | — |
| `drosteCata` | `-` | 35,998.4 ± 234.5 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 57,349.9 ± 646.7 | — | 328,640.4 | — |
| `handAna` | `-` | 30,971.4 ± 387.1 | — | 163,816.2 | — |
| `handCata` | `-` | 14,622.9 ± 127.7 | — | 0.1 | — |
| `handHylo` | `-` | 12,033.2 ± 42.1 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.5 ± 0.0 | 3.5 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 0.1 | 28.5 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 22.2 ± 0.3 | 54.5 ± 0.6 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 5.5 ± 0.0 | 5.5 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,236.9 ± 87.9 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 599.0 ± 5.5 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 54.2 ± 0.2 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,208.8 ± 26.1 | 30,138.5 ± 159.6 | 12,312.4 | 162,564.8 |
| `FoldPrices` | `size=64` | 226.4 ± 2.8 | 2,543.3 ± 13.9 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 30.7 ± 0.5 | 388.1 ± 3.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 8,025.4 ± 54.2 | 33,108.6 ± 406.8 | 36,897.3 | 176,890.3 |
| `Modify` | `size=64` | 902.3 ± 17.0 | 2,308.7 ± 13.5 | 4,640.0 | 14,408.1 |
| `Modify` | `size=8` | 107.5 ± 1.7 | 353.9 ± 13.5 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 8.7 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 97.7 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 4.8 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 83.2 ± 0.8 | — | 400.0 | — |
| `handDrillGet` | `-` | 17.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 102.7 ± 1.0 | — | 592.0 | — |
| `handServiceGet` | `-` | 18.0 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 93.0 ± 0.3 | — | 520.0 | — |

