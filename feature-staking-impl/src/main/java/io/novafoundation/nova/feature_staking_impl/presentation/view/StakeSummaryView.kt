package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeMoreActions
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatus
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatusHelper
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeTotalRewardsView
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeTotalStakedView
import kotlinx.android.synthetic.main.view_stake_summary.view.statusTapZone
import kotlin.time.ExperimentalTime

class StakeSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    sealed class Status(@StringRes val textRes: Int, @ColorRes val tintRes: Int, val extraMessage: String?) {

        class Active(eraDisplay: String) : Status(R.string.staking_nominator_status_active, R.color.green, eraDisplay)

        class Inactive(eraDisplay: String) : Status(R.string.staking_nominator_status_inactive, R.color.red, eraDisplay)

        class Waiting(val timeLeft: Long) : Status(R.string.staking_nominator_status_waiting, R.color.white_64, null)
    }

    init {
        View.inflate(context, R.layout.view_stake_summary, this)

        orientation = VERTICAL

        with(context) {
            background = addRipple(getBlurDrawable())
        }
    }

    @ExperimentalTime
    fun setElectionStatus(status: Status) {
        with(stakeSummaryStatus) {
            setCompoundDrawableTint(status.tintRes)
            setTextColorRes(status.tintRes)
            setText(status.textRes)
        }

        if (status is Status.Waiting) {
            stakeSummaryStatusHelper.startTimer(status.timeLeft)
        } else {
            stakeSummaryStatusHelper.stopTimer()
            stakeSummaryStatusHelper.text = status.extraMessage
        }
    }

    fun hideLoading() {
        stakeTotalStakedView.hideLoading()
        stakeTotalRewardsView.hideLoading()
    }

    fun setTotalStaked(inTokens: String) {
        stakeTotalStakedView.setBody(inTokens)
    }

    fun showTotalStakedFiat() {
        stakeTotalStakedView.showWholeExtraBlock()
    }

    fun hideTotalStakeFiat() {
        stakeTotalStakedView.makeExtraBlockInvisible()
    }

    fun setTotalStakedFiat(totalStake: String) {
        stakeTotalStakedView.setExtraBlockValueText(totalStake)
    }

    fun setTotalRewards(inTokens: String) {
        stakeTotalRewardsView.setBody(inTokens)
    }

    fun showTotalRewardsFiat() {
        stakeTotalRewardsView.showWholeExtraBlock()
    }

    fun hideTotalRewardsFiat() {
        stakeTotalRewardsView.makeExtraBlockInvisible()
    }

    fun setTotalRewardsFiat(totalRewards: String) {
        stakeTotalRewardsView.setExtraBlockValueText(totalRewards)
    }

    fun setStatusClickListener(listener: OnClickListener) {
        statusTapZone.setOnClickListener(listener)
    }

    fun setStakeInfoClickListener(listener: OnClickListener) {
        setOnClickListener(listener)
    }

    val moreActions: View
        get() = stakeMoreActions
}