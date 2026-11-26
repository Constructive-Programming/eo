# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `8cd105b1080c8d4337b25c08df0e7a1bc69e17d4` · date: `2026-11-26` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 12.0 ± 0.1 | 7.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 22.6 ± 0.1 | 20.1 ± 0.4 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 118.7 ± 6.6 | — | 720.0 | — |
| `ModifyCountry` | `-` | 223.3 ± 4.1 | — | 928.0 | — |
| `ModifyPartner` | `-` | 270.9 ± 4.7 | — | 984.0 | — |
| `ReadCountry` | `-` | 139.5 ± 1.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 161.6 ± 2.0 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 239.1 ± 4.4 | — | 1,208.0 | — |
| `naiveModifyCountry` | `-` | 2,035.4 ± 30.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,351.8 ± 10.1 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,816.2 ± 47.9 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,213.8 ± 9.3 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,529.0 ± 21.9 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 677.3 ± 7.8 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 491.0 ± 76.3 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 309.8 ± 1.1 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 332.0 ± 5.5 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 990.6 ± 7.5 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 976.8 ± 14.5 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 43.4 ± 0.5 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 460.6 ± 6.1 | — | 568.0 | — |
| `handwritten_stream` | `-` | 116.6 ± 0.8 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 516.6 ± 20.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,243.1 ± 48.7 | — | 4,690.7 | — |
| `ClickToJson` | `-` | 1,938.8 ± 12.9 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 569.5 ± 11.6 | — | 2,056.0 | — |
| `WideToJson` | `-` | 473.7 ± 20.1 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,003.4 ± 11.3 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,050.9 ± 25.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 702.3 ± 12.0 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,335.8 ± 9.6 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 273.6 ± 3.0 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 234.4 ± 1.4 | — | 616.0 | — |
| `circeStrictFull` | `-` | 273.6 ± 6.3 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 523.8 ± 9.9 | — | 2,586.7 | — |
| `jsoniterDefaultsFull` | `-` | 867.0 ± 14.2 | — | 4,192.0 | — |
| `jsoniterDefaultsSparse` | `-` | 259.1 ± 12.8 | — | 640.0 | — |
| `jsoniterStrictFull` | `-` | 864.5 ± 7.0 | — | 4,192.0 | — |
| `jsoniterStrictSparse` | `-` | 313.1 ± 4.9 | — | 2,888.0 | — |
| `jsoniterWithDefaults` | `-` | 526.4 ± 14.6 | — | 1,016.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 147.8 ± 1.2 | — | 832.0 | — |
| `decode_native` | `-` | 13.3 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 143.2 ± 1.2 | — | 832.0 | — |
| `encode_bridged` | `-` | 178.8 ± 6.3 | — | 1,240.0 | — |
| `encode_native` | `-` | 9.3 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 175.5 ± 2.7 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 78.6 ± 0.4 | — | 432.0 | — |
| `fieldGet_native` | `-` | 79.2 ± 1.0 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 277.0 ± 9.7 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 131.1 ± 1.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 17.3 ± 1.6 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 16.6 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.3 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.3 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.3 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 24.1 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 29.6 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.1 ± 0.0 | — | 40.0 | — |
| `modifyFCap` | `-` | 6.8 ± 0.0 | — | 56.0 | — |
| `modifyFDerivedHeld` | `-` | 12.5 ± 0.2 | — | 56.0 | — |
| `modifyFDerivedPerCall` | `-` | 12.0 ± 0.1 | — | 56.0 | — |
| `modifyFDirect` | `-` | 6.8 ± 0.1 | — | 56.0 | — |
| `modifyFEachDerivedHeld` | `-` | 94.1 ± 1.0 | — | 432.0 | — |
| `modifyFEachDerivedPerCall` | `-` | 95.1 ± 0.9 | — | 472.0 | — |
| `modifyFEachDirect` | `-` | 92.0 ± 0.4 | — | 448.0 | — |
| `modifyFEachExt` | `-` | 94.9 ± 0.6 | — | 472.0 | — |
| `modifyFExt` | `-` | 12.1 ± 0.2 | — | 56.0 | — |
| `modifyFModifyDirect` | `-` | 5.9 ± 0.1 | — | 40.0 | — |
| `modifyFModifyGeneric` | `-` | 21.2 ± 0.2 | — | 224.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 28.7 ± 1.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 20.8 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 10.8 ± 0.3 | — | 48.0 | — |
| `Patch` | `-` | 5.2 ± 0.0 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 12.8 ± 0.2 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 17.3 ± 0.1 | — | 160.0 | — |
| `TransformFocus` | `-` | 693.0 ± 25.9 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 910.4 ± 9.5 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 697.9 ± 4.3 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 7.2 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 18.0 ± 0.2 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 19.2 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 29.4 ± 0.3 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 23.1 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 11.0 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 3.8 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 12.3 ± 1.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 17.1 ± 0.2 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 656.4 ± 5.9 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 791.6 ± 3.8 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,086.0 ± 11.5 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 3.7 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 21.5 ± 0.2 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 11.9 ± 0.0 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 15.8 ± 0.0 | — | 144.0 | — |
| `handCodecModify` | `-` | 11.5 ± 0.1 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 3.7 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.5 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.5 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 375.3 ± 5.0 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 321.6 ± 2.7 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 262.9 ± 2.2 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 3.9 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 15.0 ± 0.1 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 4.3 ± 0.0 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.7 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 460.2 ± 11.9 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 465.5 ± 16.9 | — | 744.0 | — |
| `encode_hand` | `-` | 111.5 ± 0.6 | — | 768.0 | — |
| `encode_positional` | `-` | 5,306.0 ± 116.5 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,356.2 ± 87.6 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.0 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 17.2 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 31.8 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 20.6 ± 0.0 | — | 240.0 | — |
| `kitFusedModify` | `-` | 18.9 ± 0.1 | — | 112.0 | — |
| `kitFusedRead` | `-` | 3.4 ± 0.0 | — | 16.0 | — |
| `kitGenericModify` | `-` | 23.7 ± 0.2 | — | 112.0 | — |
| `kitGenericRead` | `-` | 3.7 ± 0.0 | — | 16.0 | — |
| `kitHandModify` | `-` | 14.5 ± 0.1 | — | 112.0 | — |
| `reuseLeaf` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 13.0 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 37.5 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 104.6 ± 0.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 37.0 ± 0.4 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,103.1 ± 13.6 | 1,100.8 ± 17.7 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 125.2 ± 0.5 | 125.6 ± 0.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 16.2 ± 0.2 | 16.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,910.6 ± 50.5 | 1,916.5 ± 37.2 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 120.1 ± 0.7 | 120.1 ± 0.8 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 16.6 ± 0.1 | 16.7 ± 0.3 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.4 ± 0.1 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.6 ± 0.0 | — | 16.0 | — |
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
| `Get_3` | `-` | 14.0 ± 0.1 | 7.1 ± 0.4 | 0.0 | 0.0 |
| `Get_6` | `-` | 24.7 ± 0.2 | 19.9 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.8 ± 0.0 | 2.9 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 326,015.6 ± 6,860.7 | — | 1,066,720.8 | — |
| `cModifyId` | `size=64` | 42,243.4 ± 616.2 | — | 136,291.0 | — |
| `cModifyId` | `size=8` | 6,782.1 ± 107.7 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 160,860.4 ± 1,344.8 | — | 797,917.7 | — |
| `cReadId` | `size=64` | 20,382.7 ± 243.9 | — | 101,288.6 | — |
| `cReadId` | `size=8` | 3,118.9 ± 26.3 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 160,723.6 ± 1,285.5 | — | 797,917.7 | — |
| `cReadStreet` | `size=64` | 20,421.6 ± 64.3 | — | 101,288.6 | — |
| `cReadStreet` | `size=8` | 3,170.4 ± 60.0 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 325,191.3 ± 3,457.6 | — | 1,066,668.8 | — |
| `cReplaceId` | `size=64` | 42,320.8 ± 607.4 | — | 136,243.0 | — |
| `cReplaceId` | `size=8` | 6,797.3 ± 89.7 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 271,533.6 ± 3,061.8 | — | 1,240,717.2 | — |
| `cSumPrices` | `size=64` | 33,585.9 ± 295.3 | — | 157,350.9 | — |
| `cSumPrices` | `size=8` | 4,727.5 ± 98.6 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 145.9 ± 2.7 | — | 0.0 | — |
| `jMiss` | `size=64` | 147.2 ± 3.2 | — | 0.0 | — |
| `jMiss` | `size=8` | 146.3 ± 5.0 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,767.3 ± 18.8 | — | 41,920.8 | — |
| `jModifyId` | `size=64` | 283.7 ± 6.9 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 81.0 ± 0.9 | — | 968.0 | — |
| `jReadId` | `size=512` | 30.2 ± 1.7 | — | 64.0 | — |
| `jReadId` | `size=64` | 29.3 ± 2.0 | — | 56.0 | — |
| `jReadId` | `size=8` | 29.2 ± 1.8 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 159.0 ± 1.0 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 159.3 ± 2.2 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 158.7 ± 1.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,754.6 ± 15.2 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 277.5 ± 7.3 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 79.2 ± 1.9 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 72,096.8 ± 627.3 | — | 63,670.0 | — |
| `jSumPrices` | `size=64` | 8,874.0 ± 123.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,172.0 ± 7.2 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 66.3 ± 0.7 | — | 320.0 | — |
| `MapDrillModify` | `-` | 31.2 ± 0.2 | — | 216.0 | — |
| `MapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 53.5 ± 0.3 | — | 216.0 | — |
| `handEnvUse` | `-` | 64.7 ± 0.4 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 26.9 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 50.9 ± 0.8 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.1 ± 0.0 | 3.5 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 29.3 ± 0.3 | 23.6 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 2.7 ± 0.0 | 2.7 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 13,403.9 ± 428.8 | — | 43,035.5 | — |
| `Fold_powerEach` | `size=256` | 2,921.8 ± 42.3 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 301.3 ± 2.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 61.4 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 47,006.6 ± 1,437.8 | — | 331,119.3 | — |
| `Modify_multiFocus` | `size=256` | 10,954.8 ± 402.5 | — | 76,134.1 | — |
| `Modify_multiFocus` | `size=32` | 1,079.1 ± 8.2 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 167.1 ± 11.6 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 30,987.2 ± 454.0 | — | 115,176.0 | — |
| `Modify_powerEach` | `size=256` | 7,267.4 ± 35.9 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 804.1 ± 6.9 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 145.2 ± 1.0 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,295.8 ± 62.1 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,127.4 ± 47.8 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 219.0 ± 4.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 30.7 ± 0.1 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 3,801.6 ± 107.7 | — | 16,129.0 | — |
| `naive_sumQty` | `size=256` | 701.0 ± 40.2 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 50.2 ± 0.4 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 5.5 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 27.6 ± 0.5 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 89.2 ± 0.8 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 6.9 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 21.2 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 25.9 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 6.3 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 11.6 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 127.7 ± 0.9 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 37.1 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 803.4 ± 5.9 | — | 2,888.0 | — |
| `reuseUse` | `-` | 769.1 ± 18.6 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 17.0 ± 0.9 | 17.5 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.8 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 36.9 ± 0.2 | 53.4 ± 1.0 | 136.0 | 304.0 |
| `Modify_6` | `-` | 93.0 ± 1.2 | 86.9 ± 0.4 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 30.7 ± 0.3 | 11.3 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 12.8 ± 0.1 | 13.1 ± 0.1 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.9 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.6 ± 0.1 | 2.9 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.1 ± 0.1 | 6.0 ± 0.0 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 5.9 ± 0.1 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 34,264.4 ± 775.9 | — | 169,657.9 | — |
| `ModifyNames` | `size=64` | 4,665.0 ± 45.2 | — | 20,805.5 | — |
| `ModifyNames` | `size=8` | 525.5 ± 6.8 | — | 2,584.0 | — |
| `ModifyStreet` | `size=512` | 101.4 ± 4.5 | — | 344.0 | — |
| `ModifyStreet` | `size=64` | 98.2 ± 1.0 | — | 344.0 | — |
| `ModifyStreet` | `size=8` | 98.9 ± 2.7 | — | 344.0 | — |
| `ReadStreet` | `size=512` | 26.8 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 26.7 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 27.5 ± 1.0 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 75,605.6 ± 923.6 | — | 382,783.9 | — |
| `monocleModifyNames` | `size=64` | 7,605.8 ± 70.4 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,033.7 ± 22.6 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 37,910.7 ± 389.2 | — | 169,075.0 | — |
| `monocleModifyStreet` | `size=64` | 4,923.1 ± 34.7 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 679.9 ± 13.4 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 21,195.6 ± 388.8 | — | 69,781.1 | — |
| `monocleReadStreet` | `size=64` | 2,734.7 ± 39.4 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 391.6 ± 6.4 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 50,065.8 ± 621.2 | — | 226,291.0 | — |
| `naiveModifyNames` | `size=64` | 6,305.4 ± 38.7 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 828.1 ± 7.7 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 37,878.0 ± 529.9 | — | 169,050.8 | — |
| `naiveModifyStreet` | `size=64` | 4,908.9 ± 31.6 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 686.5 ± 20.7 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 21,171.3 ± 422.5 | — | 69,781.1 | — |
| `naiveReadStreet` | `size=64` | 2,762.6 ± 22.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 388.0 ± 4.2 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 176,670.7 ± 2,839.4 | — | 622,189.6 | — |
| `Names` | `size=64` | 21,564.2 ± 426.7 | — | 78,778.3 | — |
| `Names` | `size=8` | 2,984.1 ± 67.6 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 189,844.9 ± 5,499.8 | — | 695,808.2 | — |
| `NamesIor` | `size=64` | 22,855.2 ± 432.7 | — | 86,967.7 | — |
| `NamesIor` | `size=8` | 3,006.6 ± 17.6 | — | 11,560.1 | — |
| `Street` | `size=512` | 769.1 ± 10.6 | — | 2,720.6 | — |
| `Street` | `size=64` | 748.9 ± 18.8 | — | 2,696.1 | — |
| `Street` | `size=8` | 769.6 ± 31.9 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 759.2 ± 11.2 | — | 2,736.6 | — |
| `StreetIor` | `size=64` | 771.7 ± 7.5 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 765.1 ± 9.0 | — | 2,736.0 | — |
| `directNames` | `size=512` | 174,878.8 ± 2,038.7 | — | 609,836.2 | — |
| `directNames` | `size=64` | 21,444.6 ± 387.3 | — | 77,178.3 | — |
| `directNames` | `size=8` | 2,959.9 ± 59.8 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 761.1 ± 8.4 | — | 2,728.6 | — |
| `directStreet` | `size=64` | 767.2 ± 5.5 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 763.2 ± 7.2 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 178,305.9 ± 3,369.6 | — | 613,942.9 | — |
| `hcursorNames` | `size=64` | 22,393.0 ± 465.4 | — | 77,818.4 | — |
| `hcursorNames` | `size=8` | 2,986.4 ± 66.4 | — | 10,808.1 | — |
| `hcursorStreet` | `size=512` | 794.9 ± 15.9 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 805.1 ± 13.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 798.6 ± 11.5 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 175,501.8 ± 2,585.6 | — | 1,121,768.6 | — |
| `monocleNames` | `size=64` | 19,356.2 ± 252.8 | — | 132,783.1 | — |
| `monocleNames` | `size=8` | 2,880.7 ± 34.5 | — | 19,504.1 | — |
| `monocleStreet` | `size=512` | 133,726.3 ± 1,607.9 | — | 908,025.9 | — |
| `monocleStreet` | `size=64` | 16,474.0 ± 126.1 | — | 113,830.8 | — |
| `monocleStreet` | `size=8` | 2,468.1 ± 19.4 | — | 17,080.0 | — |
| `naiveNames` | `size=512` | 153,289.0 ± 9,242.8 | — | 965,271.0 | — |
| `naiveNames` | `size=64` | 17,839.7 ± 135.0 | — | 120,846.9 | — |
| `naiveNames` | `size=8` | 2,633.2 ± 26.4 | — | 17,845.4 | — |
| `naiveStreet` | `size=512` | 141,914.4 ± 9,782.6 | — | 908,042.0 | — |
| `naiveStreet` | `size=64` | 16,578.5 ± 243.6 | — | 113,796.2 | — |
| `naiveStreet` | `size=8` | 2,466.3 ± 15.1 | — | 17,061.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 2,957.4 ± 26.2 | — | 42,026.6 | — |
| `ModifyStreet` | `size=64` | 415.3 ± 6.6 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 232.5 ± 3.0 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 160.6 ± 1.3 | — | 144.1 | — |
| `ReadStreet` | `size=64` | 160.3 ± 1.3 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 160.6 ± 1.3 | — | 128.0 | — |
| `SumPrices` | `size=512` | 71,715.4 ± 1,233.0 | — | 63,713.7 | — |
| `SumPrices` | `size=64` | 9,201.2 ± 369.1 | — | 8,121.1 | — |
| `SumPrices` | `size=8` | 1,208.1 ± 33.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 127,737.1 ± 1,300.0 | — | 333,470.4 | — |
| `monocleModifyStreet` | `size=64` | 15,330.8 ± 53.9 | — | 30,081.5 | — |
| `monocleModifyStreet` | `size=8` | 2,353.4 ± 25.7 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 72,879.1 ± 367.8 | — | 193,214.4 | — |
| `monocleReadStreet` | `size=64` | 9,123.1 ± 56.0 | — | 24,704.9 | — |
| `monocleReadStreet` | `size=8` | 1,411.5 ± 13.9 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 349,820.6 ± 3,296.1 | — | 1,178,379.4 | — |
| `monocleSumPrices` | `size=64` | 12,222.6 ± 47.3 | — | 45,825.2 | — |
| `monocleSumPrices` | `size=8` | 1,821.3 ± 37.9 | — | 6,330.7 | — |
| `naiveModifyStreet` | `size=512` | 128,087.6 ± 1,302.3 | — | 333,436.1 | — |
| `naiveModifyStreet` | `size=64` | 15,408.7 ± 117.7 | — | 30,057.6 | — |
| `naiveModifyStreet` | `size=8` | 2,354.2 ± 9.9 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 73,701.7 ± 1,509.5 | — | 193,215.1 | — |
| `naiveReadStreet` | `size=64` | 9,150.1 ± 83.3 | — | 24,704.9 | — |
| `naiveReadStreet` | `size=8` | 1,414.8 ± 11.8 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 77,638.1 ± 551.4 | — | 230,106.5 | — |
| `naiveSumPrices` | `size=64` | 9,661.3 ± 169.0 | — | 29,337.0 | — |
| `naiveSumPrices` | `size=8` | 1,467.9 ± 12.0 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 31,564.6 ± 258.1 | — | 788.4 | — |
| `nativeReadStreet` | `size=64` | 4,291.9 ± 174.8 | — | 760.5 | — |
| `nativeReadStreet` | `size=8` | 805.6 ± 4.2 | — | 760.0 | — |
| `nativeSumPrices` | `size=512` | 59,161.9 ± 512.7 | — | 147,834.7 | — |
| `nativeSumPrices` | `size=64` | 7,523.5 ± 65.8 | — | 18,720.9 | — |
| `nativeSumPrices` | `size=8` | 1,114.8 ± 14.7 | — | 2,592.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EverywhereVarName` | `n=4096` | 93,094.6 ± 576.6 | — | 491,605.0 | — |
| `EverywhereVarName` | `n=512` | 12,950.5 ± 99.4 | — | 61,449.3 | — |
| `EverywhereVarName` | `n=64` | 1,433.9 ± 20.4 | — | 7,688.0 | — |
| `EverywhereVarNameGeneric` | `n=4096` | 117,663.4 ± 1,639.8 | — | 568,092.5 | — |
| `EverywhereVarNameGeneric` | `n=512` | 17,300.1 ± 201.0 | — | 76,481.8 | — |
| `EverywhereVarNameGeneric` | `n=64` | 1,798.2 ± 19.9 | — | 8,896.0 | — |
| `TransformDeep` | `n=4096` | 119,587.6 ± 1,039.7 | — | 454,991.0 | — |
| `TransformDeep` | `n=512` | 6,737.1 ± 85.4 | — | 20,520.7 | — |
| `TransformDeep` | `n=64` | 570.6 ± 4.1 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 73,911.2 ± 704.8 | 134,523.1 ± 1,252.4 | 360,501.8 | 753,713.9 |
| `TransformExpr` | `n=512` | 9,593.2 ± 41.1 | 12,688.0 ± 132.4 | 45,057.0 | 69,585.3 |
| `TransformExpr` | `n=64` | 1,121.7 ± 7.2 | 2,073.3 ± 13.3 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 138,344.0 ± 2,214.2 | — | 766,372.7 | — |
| `TransformMDeep` | `n=512` | 17,518.6 ± 726.4 | — | 82,145.8 | — |
| `TransformMDeep` | `n=64` | 1,446.8 ± 334.9 | — | 5,800.0 | — |
| `TransformMExpr` | `n=4096` | 145,454.4 ± 2,828.4 | 134,349.8 ± 25,215.2 | 566,465.9 | 861,873.8 |
| `TransformMExpr` | `n=512` | 17,747.4 ± 100.2 | 20,408.9 ± 120.7 | 70,777.8 | 104,658.1 |
| `TransformMExpr` | `n=64` | 2,317.4 ± 147.1 | 2,309.8 ± 364.5 | 9,213.4 | 13,200.0 |
| `UniverseDeep` | `n=4096` | 75,600.7 ± 436.6 | — | 360,679.0 | — |
| `UniverseDeep` | `n=512` | 9,446.7 ± 149.1 | — | 45,185.0 | — |
| `UniverseDeep` | `n=64` | 1,525.8 ± 4.4 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 64,257.4 ± 504.6 | 2,416,671.5 ± 196,982.3 | 295,022.8 | 4,687,435.7 |
| `UniverseExpr` | `n=512` | 8,026.8 ± 37.7 | 159,454.7 ± 8,591.4 | 36,928.8 | 475,008.3 |
| `UniverseExpr` | `n=64` | 1,207.0 ± 4.6 | 13,878.6 ± 717.2 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 159,765.6 ± 12,981.7 | 2,658,228.7 ± 195,207.9 | 393,372.3 | 6,467,631.2 |
| `UniverseJson` | `n=512` | 17,925.3 ± 81.2 | 184,857.3 ± 10,007.3 | 49,193.8 | 699,914.9 |
| `UniverseJson` | `n=64` | 2,232.9 ± 16.9 | 17,482.6 ± 153.1 | 6,152.0 | 73,208.3 |
| `visitorTransformDeep` | `n=4096` | 36,697.4 ± 156.9 | — | 163,882.7 | — |
| `visitorTransformDeep` | `n=512` | 3,634.1 ± 35.2 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 349.2 ± 3.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 53,108.3 ± 639.4 | — | 360,462.7 | — |
| `visitorTransformExpr` | `n=512` | 6,507.6 ± 48.4 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 794.2 ± 6.5 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 48,132.5 ± 387.6 | — | 229,443.0 | — |
| `visitorTransformMDeep` | `n=512` | 6,035.3 ± 45.6 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 483.0 ± 3.2 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 67,711.4 ± 270.6 | — | 447,849.3 | — |
| `visitorTransformMExpr` | `n=512` | 8,339.1 ± 75.3 | — | 50,488.9 | — |
| `visitorTransformMExpr` | `n=64` | 1,031.2 ± 6.8 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 48,358.6 ± 802.1 | — | 196,699.2 | — |
| `visitorUniverseDeep` | `n=512` | 5,797.9 ± 39.9 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 621.1 ± 5.1 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 45,894.5 ± 924.2 | — | 196,649.4 | — |
| `visitorUniverseExpr` | `n=512` | 5,567.1 ± 45.5 | — | 24,584.6 | — |
| `visitorUniverseExpr` | `n=64` | 632.4 ± 2.9 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 73,938.4 ± 612.9 | — | 196,685.8 | — |
| `visitorUniverseJson` | `n=512` | 7,662.3 ± 49.8 | — | 24,600.8 | — |
| `visitorUniverseJson` | `n=64` | 1,055.8 ± 9.2 | — | 3,096.0 | — |

## PlatedChildrenBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ChildrenMany` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `ChildrenPair` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `MapChildrenMany` | `-` | 8.4 ± 0.1 | — | 64.0 | — |
| `handChildrenMany` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handChildrenPair` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `handMapChildrenMany` | `-` | 6.8 ± 0.1 | — | 64.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 11,030.6 ± 55.9 | — | 41,413.2 | — |
| `Modify_powerEach` | `size=16` | 209.5 ± 2.3 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,755.3 ± 54.9 | — | 10,688.4 | — |
| `Modify_powerEach` | `size=4` | 91.0 ± 1.1 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 43,548.5 ± 992.0 | — | 164,358.9 | — |
| `Modify_powerEach` | `size=64` | 629.2 ± 4.2 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 46,780.6 ± 623.1 | — | 279,423.8 | — |
| `monocle_powerEach` | `size=16` | 441.5 ± 16.1 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 16,962.0 ± 120.5 | — | 107,330.6 | — |
| `monocle_powerEach` | `size=4` | 152.4 ± 7.6 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 144,545.6 ± 5,466.6 | — | 967,787.4 | — |
| `monocle_powerEach` | `size=64` | 1,748.5 ± 26.3 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 5,653.2 ± 25.5 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 83.9 ± 0.4 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,339.9 ± 9.0 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 20.8 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,382.1 ± 116.0 | — | 114,780.5 | — |
| `naive_powerEach` | `size=64` | 329.6 ± 1.2 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 57,892.2 ± 4,097.9 | — | 210,441.4 | — |
| `Modify_nested` | `size=16` | 1,091.2 ± 4.6 | — | 4,768.0 | — |
| `Modify_nested` | `size=256` | 15,678.2 ± 187.6 | — | 53,759.2 | — |
| `Modify_nested` | `size=4` | 493.6 ± 3.8 | — | 2,216.0 | — |
| `Modify_nested` | `size=64` | 4,059.2 ± 50.7 | — | 14,693.8 | — |
| `monocle_nested` | `size=1024` | 191,943.3 ± 1,044.5 | — | 1,118,749.4 | — |
| `monocle_nested` | `size=16` | 1,965.0 ± 43.4 | — | 15,749.4 | — |
| `monocle_nested` | `size=256` | 72,022.1 ± 762.6 | — | 430,203.0 | — |
| `monocle_nested` | `size=4` | 866.4 ± 11.6 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 7,789.8 ± 144.8 | — | 58,859.5 | — |
| `naive_nested` | `size=1024` | 20,512.4 ± 89.3 | — | 115,069.4 | — |
| `naive_nested` | `size=16` | 299.7 ± 4.1 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,182.8 ± 30.1 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 105.7 ± 1.8 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,202.9 ± 12.2 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,062.3 ± 8.0 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 20,600.3 ± 382.7 | — | 104,680.3 | — |
| `Modify_sparse` | `size=32` | 278.5 ± 2.4 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,170.9 ± 24.2 | — | 24,785.4 | — |
| `Modify_sparse` | `size=8` | 94.2 ± 1.2 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,442.0 ± 41.0 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 79,209.3 ± 1,772.7 | — | 523,133.0 | — |
| `monocle_sparse` | `size=32` | 770.6 ± 6.2 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 26,244.3 ± 438.6 | — | 166,679.9 | — |
| `monocle_sparse` | `size=8` | 230.8 ± 2.7 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 246.6 ± 1.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,216.3 ± 18.4 | — | 24,611.7 | — |
| `naive_sparse` | `size=32` | 63.3 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,066.3 ± 9.7 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 19.0 ± 0.1 | — | 128.0 | — |

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
| `ReverseGet_3` | `-` | 15.9 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 27.8 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 5.3 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 8.8 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 73,920.6 ± 736.5 | — | 589,712.5 | — |
| `Cata` | `-` | 46,956.6 ± 588.7 | — | 99,288.3 | — |
| `Hylo` | `-` | 68,543.6 ± 513.2 | — | 295,848.5 | — |
| `drosteAna` | `-` | 36,315.9 ± 515.3 | — | 327,632.3 | — |
| `drosteCata` | `-` | 31,271.1 ± 350.4 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 38,946.8 ± 401.8 | — | 328,640.3 | — |
| `handAna` | `-` | 19,830.4 ± 737.4 | — | 163,816.1 | — |
| `handCata` | `-` | 10,375.1 ± 72.1 | — | 0.1 | — |
| `handHylo` | `-` | 7,951.4 ± 568.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.9 ± 0.0 | 1.9 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 9.6 ± 0.1 | 21.6 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 20.3 ± 2.3 | 44.8 ± 0.4 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,646.6 ± 57.1 | — | 20,200.7 | — |
| `FoldNested` | `size=64` | 421.0 ± 3.2 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 39.8 ± 0.3 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,773.3 ± 36.1 | 24,740.8 ± 241.4 | 12,312.3 | 162,563.9 |
| `FoldPrices` | `size=64` | 120.1 ± 1.0 | 1,745.8 ± 21.8 | 1,560.0 | 15,408.0 |
| `FoldPrices` | `size=8` | 16.7 ± 0.2 | 232.1 ± 1.1 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 6,892.6 ± 185.6 | 26,817.2 ± 302.0 | 36,897.1 | 176,885.9 |
| `Modify` | `size=64` | 658.1 ± 3.6 | 1,528.5 ± 15.2 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 81.2 ± 2.0 | 216.6 ± 4.1 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 9.0 ± 0.8 | — | 0.0 | — |
| `DrillModify` | `-` | 73.1 ± 1.2 | — | 472.0 | — |
| `ServiceGet` | `-` | 4.0 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 49.5 ± 0.3 | — | 400.0 | — |
| `handDrillGet` | `-` | 12.0 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 67.2 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 11.6 ± 0.0 | — | 120.0 | — |
| `handServiceReplace` | `-` | 54.7 ± 0.4 | — | 520.0 | — |

