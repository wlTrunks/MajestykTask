package com.majestykapps.arch.presentation.util

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.observe
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * View binding delegate, it will unbind clear binding on destroy fragment
 */
internal fun <B : ViewBinding> Fragment.bindingDelegate(
    bindingProvider: (view: View) -> B
): BindingDelegate<B> =
    BindingDelegate(
        this,
        bindingProvider
    )

internal class BindingDelegate<B : ViewBinding>(
    fragment: Fragment,
    private val bindingProvider: (view: View) -> B
) : ReadOnlyProperty<Fragment, B>, LifecycleObserver {

    private var binding: B? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { it?.lifecycle?.addObserver(this) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    internal fun onDestroyView() {
        binding = null
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): B =
        binding ?: bindingProvider(thisRef.requireView()).apply { binding = this }
}

internal fun View.visibleIf(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

internal fun Fragment.showSnack(message: String?, block: () -> Unit): Snackbar {
    return Snackbar.make(
        requireView(), message ?: "Response Error",
        Snackbar.LENGTH_INDEFINITE
    )
        .setAction("Reload") { block.invoke() }
}

/**
 * Shows the on screen keyboard.
 */
internal fun View?.showKeyboard() {
    this?.let {
        if (it.requestFocus()) {
            context.getSystemService<InputMethodManager>()?.showSoftInput(it, 0)
        }
    }
}

