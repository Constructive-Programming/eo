package dev.constructive.eo
package data

/** Fixed-index carrier: the leftover `X` and a focus tabulation over `I`. */
type GlassF[I] = [X, A] =>> (context: X, values: I => A)
