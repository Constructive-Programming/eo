# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `8ad0d66e1634d08b9b0caef85b57e595a0e015af` · date: `2026-11-12` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.6 ± 0.2 | 9.8 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.8 ± 0.6 | 25.8 ± 3.6 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.2 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 163.1 ± 3.7 | — | 720.0 | — |
| `ModifyCountry` | `-` | 307.2 ± 7.5 | — | 928.0 | — |
| `ModifyPartner` | `-` | 403.7 ± 12.3 | — | 994.7 | — |
| `ReadCountry` | `-` | 175.4 ± 3.3 | — | 520.0 | — |
| `ReadPartner` | `-` | 216.5 ± 4.3 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 314.0 ± 9.3 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,645.9 ± 32.3 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,908.1 ± 41.9 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,866.1 ± 46.6 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,666.8 ± 37.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,093.8 ± 54.4 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 950.0 ± 4.9 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 621.3 ± 6.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 404.0 ± 3.9 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 436.7 ± 2.9 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,370.5 ± 34.1 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,342.5 ± 21.3 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 54.2 ± 0.6 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 632.3 ± 16.0 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.4 ± 1.1 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 658.7 ± 15.2 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,953.8 ± 18.8 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,867.8 ± 170.9 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 871.9 ± 17.9 | — | 2,024.0 | — |
| `WideToJson` | `-` | 683.9 ± 21.5 | — | 1,450.7 | — |
| `naiveClickToAvro` | `-` | 1,454.6 ± 15.6 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,927.7 ± 23.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 975.9 ± 11.2 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,881.8 ± 22.9 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 377.0 ± 15.5 | — | 1,248.0 | — |
| `circeDefaultsSparse` | `-` | 324.8 ± 4.0 | — | 813.4 | — |
| `circeStrictFull` | `-` | 361.8 ± 5.0 | — | 1,221.3 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 707.9 ± 8.3 | — | 2,416.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,260.8 ± 12.1 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 330.9 ± 5.9 | — | 736.0 | — |
| `jsoniterStrictFull` | `-` | 1,254.0 ± 22.8 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 509.7 ± 3.5 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 729.0 ± 10.4 | — | 1,016.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 193.1 ± 1.8 | — | 832.0 | — |
| `decode_native` | `-` | 17.4 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 189.0 ± 3.8 | — | 832.0 | — |
| `encode_bridged` | `-` | 252.1 ± 4.7 | — | 1,240.0 | — |
| `encode_native` | `-` | 12.1 ± 0.6 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 259.0 ± 3.6 | — | 1,272.0 | — |
| `fieldGet_bridged` | `-` | 100.6 ± 0.9 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.2 ± 1.1 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 398.2 ± 4.0 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 168.5 ± 3.4 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.7 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.1 ± 0.4 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.2 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.3 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.5 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 12.9 ± 0.1 | — | 48.0 | — |
| `Patch` | `-` | 6.5 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.2 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 21.0 ± 0.2 | — | 160.0 | — |
| `TransformFocus` | `-` | 833.3 ± 11.9 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,171.8 ± 59.8 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 905.3 ± 6.1 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.5 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 22.7 ± 0.2 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 23.0 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.6 ± 0.3 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.4 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.5 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.9 ± 0.1 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.2 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 810.4 ± 4.6 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 958.1 ± 6.9 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,343.9 ± 11.3 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.4 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 29.3 ± 0.9 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.4 ± 0.1 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.8 ± 0.1 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.2 ± 0.1 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 414.9 ± 4.1 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 398.9 ± 6.4 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 271.8 ± 3.3 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.1 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.7 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 521.4 ± 11.0 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 537.3 ± 12.9 | — | 744.0 | — |
| `encode_hand` | `-` | 141.2 ± 1.0 | — | 768.0 | — |
| `encode_positional` | `-` | 6,399.6 ± 83.2 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,720.8 ± 64.4 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 38.4 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.8 ± 0.1 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.3 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.5 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.7 ± 2.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.2 ± 0.4 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,242.0 ± 11.7 | 1,244.4 ± 14.4 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.9 ± 0.7 | 157.0 ± 1.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.5 ± 0.1 | 20.9 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,141.0 ± 21.3 | 2,132.2 ± 17.9 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 156.0 ± 1.0 | 158.5 ± 1.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.7 ± 0.1 | 26.5 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
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
| `Get_3` | `-` | 16.3 ± 0.1 | 9.0 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 31.9 ± 4.5 | 25.8 ± 1.9 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 405,017.9 ± 5,393.6 | — | 1,066,745.3 | — |
| `cModifyId` | `size=64` | 52,113.9 ± 1,629.1 | — | 136,293.7 | — |
| `cModifyId` | `size=8` | 8,478.3 ± 83.7 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 208,171.0 ± 1,997.5 | — | 797,931.2 | — |
| `cReadId` | `size=64` | 26,004.6 ± 321.2 | — | 101,294.2 | — |
| `cReadId` | `size=8` | 3,976.4 ± 70.8 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 204,443.7 ± 3,161.1 | — | 797,930.1 | — |
| `cReadStreet` | `size=64` | 26,159.0 ± 437.8 | — | 101,294.4 | — |
| `cReadStreet` | `size=8` | 4,097.5 ± 104.2 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 408,094.0 ± 7,611.5 | — | 1,066,707.3 | — |
| `cReplaceId` | `size=64` | 51,914.8 ± 1,220.5 | — | 136,237.8 | — |
| `cReplaceId` | `size=8` | 8,535.6 ± 149.3 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 339,819.4 ± 2,869.2 | — | 1,240,739.4 | — |
| `cSumPrices` | `size=64` | 41,652.2 ± 404.6 | — | 157,064.1 | — |
| `cSumPrices` | `size=8` | 6,263.5 ± 146.5 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 183.5 ± 12.6 | — | 0.1 | — |
| `jMiss` | `size=64` | 172.8 ± 3.9 | — | 0.0 | — |
| `jMiss` | `size=8` | 182.6 ± 4.0 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,868.6 ± 66.0 | — | 41,929.1 | — |
| `jModifyId` | `size=64` | 493.8 ± 8.3 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 113.6 ± 1.7 | — | 976.0 | — |
| `jReadId` | `size=512` | 34.2 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.9 ± 2.8 | — | 56.0 | — |
| `jReadId` | `size=8` | 36.0 ± 2.9 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 195.7 ± 3.9 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 197.2 ± 3.1 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 195.7 ± 3.1 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,828.8 ± 71.8 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 501.5 ± 6.7 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 113.9 ± 4.0 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 84,866.6 ± 1,021.2 | — | 63,661.8 | — |
| `jSumPrices` | `size=64` | 12,478.8 ± 705.4 | — | 8,120.4 | — |
| `jSumPrices` | `size=8` | 1,540.4 ± 79.4 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 85.8 ± 0.5 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.0 ± 0.2 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 65.6 ± 2.2 | — | 216.0 | — |
| `handEnvUse` | `-` | 86.6 ± 1.9 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.2 ± 0.4 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 62.9 ± 0.6 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.4 ± 0.7 | 29.9 ± 0.3 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.1 | 3.5 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,449.3 ± 509.2 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,190.8 ± 92.4 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 372.9 ± 4.0 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 86.4 ± 0.4 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 52,996.6 ± 1,054.1 | — | 331,155.1 | — |
| `Modify_multiFocus` | `size=256` | 11,322.8 ± 161.6 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,375.3 ± 18.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 227.3 ± 14.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,006.5 ± 732.2 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 8,268.7 ± 101.0 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,029.2 ± 37.1 | — | 3,168.0 | — |
| `Modify_powerEach` | `size=4` | 197.2 ± 3.0 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,399.8 ± 90.6 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,096.2 ± 17.3 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 251.7 ± 0.8 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.5 ± 0.8 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,090.2 ± 64.1 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 717.4 ± 35.6 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.4 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.6 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.9 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 123.3 ± 0.7 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 9.0 ± 0.2 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.8 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.7 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.0 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.9 ± 0.3 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 157.6 ± 0.7 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.5 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,117.9 ± 18.8 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,089.0 ± 42.2 | — | 2,712.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.1 | 21.6 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 46.7 ± 2.4 | 73.0 ± 1.2 | 136.0 | 304.0 |
| `Modify_6` | `-` | 113.3 ± 4.2 | 120.5 ± 1.1 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 42.0 ± 0.2 | 13.7 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 14.6 ± 0.8 | 16.4 ± 0.2 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.6 ± 0.0 | 3.8 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.9 ± 0.2 | 7.3 ± 0.1 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 8.5 ± 0.2 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,326.1 ± 1,042.3 | — | 97,423.6 | — |
| `ModifyNames` | `size=64` | 4,031.4 ± 98.2 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 544.4 ± 5.4 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 111.3 ± 0.9 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 114.7 ± 7.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.5 ± 1.2 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.4 ± 0.9 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.0 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,394.5 ± 1,930.6 | — | 382,775.2 | — |
| `monocleModifyNames` | `size=64` | 8,994.6 ± 106.9 | — | 39,856.3 | — |
| `monocleModifyNames` | `size=8` | 1,453.1 ± 102.1 | — | 5,405.3 | — |
| `monocleModifyStreet` | `size=512` | 45,849.4 ± 477.0 | — | 169,078.9 | — |
| `monocleModifyStreet` | `size=64` | 6,020.8 ± 40.3 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 918.5 ± 8.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,355.0 ± 250.5 | — | 69,783.6 | — |
| `monocleReadStreet` | `size=64` | 3,241.9 ± 31.0 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 475.8 ± 4.0 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,609.1 ± 343.2 | — | 226,296.1 | — |
| `naiveModifyNames` | `size=64` | 7,642.6 ± 77.9 | — | 27,929.1 | — |
| `naiveModifyNames` | `size=8` | 1,080.4 ± 11.9 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,913.0 ± 325.8 | — | 169,056.8 | — |
| `naiveModifyStreet` | `size=64` | 5,977.5 ± 48.1 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 928.6 ± 14.7 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,107.0 ± 245.1 | — | 69,783.5 | — |
| `naiveReadStreet` | `size=64` | 3,236.9 ± 23.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 479.3 ± 5.9 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 249,763.2 ± 3,420.2 | — | 626,352.2 | — |
| `Names` | `size=64` | 30,723.0 ± 688.9 | — | 78,779.2 | — |
| `Names` | `size=8` | 4,284.5 ± 65.4 | — | 10,949.4 | — |
| `NamesIor` | `size=512` | 265,061.2 ± 6,513.3 | — | 691,764.4 | — |
| `NamesIor` | `size=64` | 34,436.9 ± 1,641.8 | — | 86,455.6 | — |
| `NamesIor` | `size=8` | 4,409.5 ± 64.8 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,059.8 ± 43.0 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,058.2 ± 20.9 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,060.4 ± 20.8 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,082.3 ± 36.6 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,082.1 ± 13.7 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,052.1 ± 16.7 | — | 2,736.0 | — |
| `directNames` | `size=512` | 245,194.7 ± 5,164.7 | — | 609,892.5 | — |
| `directNames` | `size=64` | 30,080.2 ± 495.6 | — | 77,707.1 | — |
| `directNames` | `size=8` | 4,136.7 ± 124.7 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,084.5 ± 44.5 | — | 2,736.9 | — |
| `directStreet` | `size=64` | 1,093.3 ± 10.9 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,062.4 ± 14.9 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 241,164.4 ± 3,022.4 | — | 609,889.3 | — |
| `hcursorNames` | `size=64` | 29,958.4 ± 641.2 | — | 77,792.2 | — |
| `hcursorNames` | `size=8` | 4,159.7 ± 30.0 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,129.8 ± 11.0 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,161.1 ± 9.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,103.2 ± 11.4 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 223,578.8 ± 3,035.0 | — | 1,121,768.9 | — |
| `monocleNames` | `size=64` | 24,135.7 ± 187.8 | — | 132,765.3 | — |
| `monocleNames` | `size=8` | 3,705.5 ± 36.3 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 177,949.8 ± 1,717.1 | — | 908,023.6 | — |
| `monocleStreet` | `size=64` | 21,395.8 ± 407.1 | — | 113,121.9 | — |
| `monocleStreet` | `size=8` | 3,235.3 ± 101.0 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 191,745.8 ± 1,730.8 | — | 965,264.8 | — |
| `naiveNames` | `size=64` | 22,549.3 ± 259.3 | — | 120,831.6 | — |
| `naiveNames` | `size=8` | 3,373.1 ± 40.9 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 181,874.9 ± 5,663.3 | — | 908,036.9 | — |
| `naiveStreet` | `size=64` | 21,271.4 ± 202.3 | — | 113,775.2 | — |
| `naiveStreet` | `size=8` | 3,184.9 ± 33.6 | — | 17,056.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,082.3 ± 47.2 | — | 42,027.6 | — |
| `ModifyStreet` | `size=64` | 698.4 ± 6.5 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 306.9 ± 9.3 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 198.4 ± 4.6 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 197.0 ± 1.5 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 196.1 ± 1.3 | — | 128.0 | — |
| `SumPrices` | `size=512` | 87,168.2 ± 2,996.6 | — | 63,713.8 | — |
| `SumPrices` | `size=64` | 10,962.5 ± 394.2 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,589.9 ± 105.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 160,413.7 ± 3,114.4 | — | 333,566.7 | — |
| `monocleModifyStreet` | `size=64` | 19,102.6 ± 900.1 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,149.2 ± 26.9 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 89,188.3 ± 539.7 | — | 193,228.3 | — |
| `monocleReadStreet` | `size=64` | 11,325.6 ± 189.2 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,826.7 ± 30.6 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 419,394.4 ± 7,146.1 | — | 1,181,512.1 | — |
| `monocleSumPrices` | `size=64` | 15,265.1 ± 386.0 | — | 45,833.6 | — |
| `monocleSumPrices` | `size=8` | 2,420.0 ± 39.5 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,529.8 ± 1,389.0 | — | 333,563.0 | — |
| `naiveModifyStreet` | `size=64` | 18,853.9 ± 214.2 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,154.6 ± 63.1 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,616.4 ± 860.4 | — | 193,227.9 | — |
| `naiveReadStreet` | `size=64` | 11,539.8 ± 131.3 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,786.2 ± 10.9 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 93,399.0 ± 1,072.6 | — | 230,120.0 | — |
| `naiveSumPrices` | `size=64` | 11,947.7 ± 193.6 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,871.3 ± 19.8 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,444.5 ± 369.3 | — | 451.8 | — |
| `nativeReadStreet` | `size=64` | 4,302.9 ± 96.5 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 816.3 ± 12.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 63,452.1 ± 6,151.4 | — | 86,272.4 | — |
| `nativeSumPrices` | `size=64` | 7,764.1 ± 99.7 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,169.6 ± 28.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 134,976.0 ± 10,614.1 | — | 455,002.2 | — |
| `TransformDeep` | `n=512` | 7,403.8 ± 99.4 | — | 20,520.8 | — |
| `TransformDeep` | `n=64` | 727.2 ± 6.7 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 89,149.0 ± 517.2 | 168,565.7 ± 8,595.7 | 360,512.9 | 753,738.7 |
| `TransformExpr` | `n=512` | 11,830.3 ± 112.1 | 15,314.2 ± 92.1 | 45,057.2 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,374.5 ± 12.3 | 2,523.9 ± 26.5 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 168,845.4 ± 5,319.4 | — | 766,394.9 | — |
| `TransformMDeep` | `n=512` | 18,826.4 ± 205.4 | — | 82,145.9 | — |
| `TransformMDeep` | `n=64` | 1,793.3 ± 11.9 | — | 7,138.7 | — |
| `TransformMExpr` | `n=4096` | 170,490.7 ± 1,561.5 | 193,423.1 ± 14,811.8 | 566,484.1 | 849,948.8 |
| `TransformMExpr` | `n=512` | 22,942.2 ± 2,387.3 | 25,402.9 ± 255.9 | 70,778.3 | 104,658.6 |
| `TransformMExpr` | `n=64` | 2,684.3 ± 61.4 | 3,149.3 ± 28.7 | 9,634.7 | 13,008.1 |
| `UniverseDeep` | `n=4096` | 89,726.7 ± 1,829.1 | — | 360,689.3 | — |
| `UniverseDeep` | `n=512` | 10,875.7 ± 69.3 | — | 45,185.1 | — |
| `UniverseDeep` | `n=64` | 1,752.6 ± 11.0 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 80,470.0 ± 1,630.7 | 2,939,507.7 ± 197,499.8 | 295,034.6 | 4,687,815.1 |
| `UniverseExpr` | `n=512` | 9,877.5 ± 126.1 | 201,878.2 ± 8,248.5 | 36,929.0 | 475,012.6 |
| `UniverseExpr` | `n=64` | 1,451.6 ± 13.9 | 17,941.3 ± 586.3 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 197,310.8 ± 978.3 | 3,119,448.1 ± 146,299.9 | 393,399.6 | 6,489,810.7 |
| `UniverseJson` | `n=512` | 22,885.1 ± 171.4 | 248,842.9 ± 2,164.0 | 49,194.3 | 699,921.4 |
| `UniverseJson` | `n=64` | 2,930.5 ± 21.4 | 22,044.5 ± 336.4 | 6,152.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,080.6 ± 337.4 | — | 163,886.6 | — |
| `visitorTransformDeep` | `n=512` | 4,890.4 ± 47.9 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 430.1 ± 4.9 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 63,811.1 ± 719.2 | — | 360,470.4 | — |
| `visitorTransformExpr` | `n=512` | 7,565.0 ± 54.5 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 988.6 ± 8.8 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 66,360.7 ± 10,771.3 | — | 338,693.6 | — |
| `visitorTransformMDeep` | `n=512` | 6,260.2 ± 35.1 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 579.3 ± 3.9 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 87,530.7 ± 911.8 | — | 447,863.7 | — |
| `visitorTransformMExpr` | `n=512` | 10,138.9 ± 85.6 | — | 50,489.0 | — |
| `visitorTransformMExpr` | `n=64` | 1,339.3 ± 15.5 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 53,940.3 ± 570.6 | — | 196,703.3 | — |
| `visitorUniverseDeep` | `n=512` | 6,483.9 ± 49.6 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 747.1 ± 5.3 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,850.8 ± 361.1 | — | 196,654.5 | — |
| `visitorUniverseExpr` | `n=512` | 6,518.7 ± 43.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 796.0 ± 2.6 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 101,133.5 ± 863.3 | — | 196,705.6 | — |
| `visitorUniverseJson` | `n=512` | 9,223.5 ± 53.1 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,349.4 ± 10.6 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,221.8 ± 96.9 | — | 41,413.8 | — |
| `Modify_powerEach` | `size=16` | 260.0 ± 2.4 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,994.8 ± 27.5 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.3 ± 0.9 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,396.0 ± 1,946.1 | — | 164,371.7 | — |
| `Modify_powerEach` | `size=64` | 771.1 ± 9.4 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 59,967.3 ± 989.0 | — | 279,432.8 | — |
| `monocle_powerEach` | `size=16` | 601.7 ± 4.3 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,658.0 ± 310.0 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 207.8 ± 1.2 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 175,543.0 ± 1,073.3 | — | 967,837.8 | — |
| `monocle_powerEach` | `size=64` | 2,120.7 ± 45.3 | — | 14,509.4 | — |
| `naive_powerEach` | `size=1024` | 6,709.0 ± 26.9 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 100.8 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,619.9 ± 8.5 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.1 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,799.0 ± 83.8 | — | 114,787.6 | — |
| `naive_powerEach` | `size=64` | 396.9 ± 3.4 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 72,148.2 ± 1,610.7 | — | 210,515.3 | — |
| `Modify_nested` | `size=16` | 1,443.5 ± 18.4 | — | 4,720.1 | — |
| `Modify_nested` | `size=256` | 17,684.8 ± 311.1 | — | 53,707.1 | — |
| `Modify_nested` | `size=4` | 695.7 ± 15.7 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,576.3 ± 30.2 | — | 14,693.9 | — |
| `monocle_nested` | `size=1024` | 251,551.7 ± 3,627.0 | — | 1,118,858.0 | — |
| `monocle_nested` | `size=16` | 2,610.3 ± 30.5 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 91,767.6 ± 3,506.8 | — | 430,208.5 | — |
| `monocle_nested` | `size=4` | 1,406.2 ± 21.3 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 8,850.7 ± 216.4 | — | 58,860.4 | — |
| `naive_nested` | `size=1024` | 21,868.0 ± 477.0 | — | 115,071.8 | — |
| `naive_nested` | `size=16` | 365.1 ± 3.2 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,237.7 ± 28.8 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 130.0 ± 2.6 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,326.4 ± 11.8 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,330.3 ± 16.6 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,998.3 ± 862.1 | — | 104,691.9 | — |
| `Modify_sparse` | `size=32` | 353.6 ± 5.5 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,698.3 ± 42.1 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 120.2 ± 1.1 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,862.2 ± 32.2 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 107,176.6 ± 1,768.7 | — | 523,147.0 | — |
| `monocle_sparse` | `size=32` | 1,079.2 ± 10.2 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,115.3 ± 269.8 | — | 166,685.7 | — |
| `monocle_sparse` | `size=8` | 346.6 ± 4.9 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 287.0 ± 2.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,960.7 ± 50.8 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 74.4 ± 0.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,165.5 ± 8.5 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.0 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.1 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.0 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.7 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 96,454.1 ± 877.8 | — | 589,712.7 | — |
| `Cata` | `-` | 60,222.3 ± 654.4 | — | 99,288.4 | — |
| `Hylo` | `-` | 90,466.8 ± 1,266.3 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,349.0 ± 419.0 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,179.7 ± 253.0 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 65,094.2 ± 548.7 | — | 328,640.5 | — |
| `handAna` | `-` | 20,655.8 ± 231.0 | — | 163,816.1 | — |
| `handCata` | `-` | 12,672.2 ± 52.2 | — | 0.1 | — |
| `handHylo` | `-` | 9,571.0 ± 77.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.2 ± 0.1 | 27.9 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.7 ± 0.3 | 65.5 ± 2.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,040.4 ± 79.0 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 534.7 ± 7.2 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 49.3 ± 0.5 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,878.0 ± 30.1 | 31,263.1 ± 314.0 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 155.6 ± 1.2 | 2,055.6 ± 6.3 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.6 ± 0.2 | 321.3 ± 4.8 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,304.2 ± 223.1 | 33,342.2 ± 251.2 | 36,897.2 | 176,890.8 |
| `Modify` | `size=64` | 840.3 ± 4.3 | 1,808.0 ± 11.9 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 95.0 ± 0.8 | 281.4 ± 1.6 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.4 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 93.5 ± 0.6 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.4 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 65.5 ± 0.7 | — | 400.0 | — |
| `handDrillGet` | `-` | 13.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 90.2 ± 0.3 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.7 ± 0.8 | — | 520.0 | — |

