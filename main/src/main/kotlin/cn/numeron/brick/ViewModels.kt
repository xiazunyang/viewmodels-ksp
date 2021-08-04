package cn.numeron.brick

import androidx.lifecycle.ViewModel
import kotlin.coroutines.CoroutineContext

class StringViewModel(id: String?) : ViewModel()

class LambdaViewModel(provider: () -> String?) : ViewModel()

class PairViewModel(pair: Pair<String, List<String>>?) : ViewModel()
