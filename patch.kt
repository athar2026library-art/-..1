    val currentZekr = azkarList[currentIndex]
    
    val currentIdx by rememberUpdatedState(currentIndex)
    val currentRem by rememberUpdatedState(countRemaining)
    
    DisposableEffect(category) {
        onDispose {
            if (isInitialized && currentIdx < azkarList.size) {
                viewModel.saveLastReadState(category, currentIdx, currentRem)
            }
        }
    }

    LaunchedEffect(currentIndex) {
        if (isInitialized && currentIndex < azkarList.size) {
            viewModel.saveLastReadState(category, currentIndex, currentZekr.repeatCount)
        }
    }
