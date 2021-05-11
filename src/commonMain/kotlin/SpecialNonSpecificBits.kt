
typealias FormattingFunction<T> = (T) -> CharSequence?
typealias FormattingFunction2<T> = (T) -> CharSequence
typealias specialConsumer = (String) -> Unit
typealias MIntConsumer = (Int) -> Unit
typealias MDoubleConsumer = (Double) -> Unit
typealias GenericConsumer<T> = (T) -> Unit
typealias GenericPredicate<T> = (T) -> Boolean