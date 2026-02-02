package io.github.abc15018045126.sora.widget.component

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

class DefaultCompletionLayout : CompletionLayout {

    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var rootView: LinearLayout
    private var editorAutoCompletion: EditorAutoCompletion? = null

    override fun setEditorCompletion(completion: EditorAutoCompletion) {
        editorAutoCompletion = completion
    }

    override fun setEnabledAnimation(enabledAnimation: Boolean) {
        if (enabledAnimation) {
            val transition = LayoutTransition()
            transition.enableTransitionType(LayoutTransition.CHANGING)
            transition.enableTransitionType(LayoutTransition.APPEARING)
            transition.enableTransitionType(LayoutTransition.DISAPPEARING)
            transition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
            transition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
            transition.addTransitionListener(object : LayoutTransition.TransitionListener {
                override fun startTransition(
                    transition: LayoutTransition,
                    container: ViewGroup,
                    view: View,
                    transitionType: Int
                ) {
                }

                override fun endTransition(
                    transition: LayoutTransition,
                    container: ViewGroup,
                    view: View,
                    transitionType: Int
                ) {
                    if (view !== listView) {
                        return
                    }
                    view.requestLayout()
                }
            })
            rootView.layoutTransition = transition
            listView.layoutTransition = transition
        } else {
            rootView.layoutTransition = null
            listView.layoutTransition = null
        }
    }

    override fun inflate(context: Context): View {
        val rootLayout = LinearLayout(context)
        rootView = rootLayout
        listView = ListView(context)
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)

        rootLayout.orientation = LinearLayout.VERTICAL

        setEnabledAnimation(false)

        rootLayout.addView(
            progressBar,
            LinearLayout.LayoutParams(
                -1,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f,
                    context.resources.displayMetrics
                ).toInt()
            )
        )
        rootLayout.addView(listView, LinearLayout.LayoutParams(-1, -1))

        progressBar.isIndeterminate = true
        val progressBarLayoutParams = progressBar.layoutParams as LinearLayout.LayoutParams

        progressBarLayoutParams.topMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            -8f,
            context.resources.displayMetrics
        ).toInt()
        progressBarLayoutParams.bottomMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            -8f,
            context.resources.displayMetrics
        ).toInt()
        progressBarLayoutParams.leftMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        progressBarLayoutParams.rightMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()

        val gd = GradientDrawable()
        gd.cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            context.resources.displayMetrics
        )

        rootLayout.background = gd

        setRootViewOutlineProvider(rootView)

        listView.dividerHeight = 0
        setLoading(true)

        listView.setOnItemClickListener { _, _, position, _ ->
            try {
                editorAutoCompletion?.selectCompletion(position)
            } catch (e: Exception) {
                e.printStackTrace(System.err)
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }


        return rootLayout
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        val gd = GradientDrawable()
        val context = editorAutoCompletion!!.editor.context
        gd.cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            context.resources.displayMetrics
        )
        gd.setStroke(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                context.resources.displayMetrics
            ).toInt(),
            colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER)
        )
        gd.setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
        rootView.background = gd

        setRootViewOutlineProvider(rootView)
    }

    override fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun getCompletionList(): ListView {
        return listView
    }

    /**
     * Perform motion events
     */
    private fun performScrollList(offset: Int) {
        val adpView = getCompletionList()

        val down = SystemClock.uptimeMillis()
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()
    }

    private fun setRootViewOutlineProvider(rootView: View) {
        rootView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        view.context.resources.displayMetrics
                    )
                )
            }
        }
        rootView.clipToOutline = true
    }

    override fun ensureListPositionVisible(position: Int, increment: Int) {
        listView.post {
            // Used for reset scroll position
            if (position == 0 && increment == 0) {
                listView.setSelectionFromTop(0, 0)
                return@post
            }
            while (listView.firstVisiblePosition + 1 > position && listView.canScrollList(-1)) {
                performScrollList(increment / 2)
            }
            while (listView.lastVisiblePosition - 1 < position && listView.canScrollList(1)) {
                performScrollList(-increment / 2)
            }
        }
    }
}
