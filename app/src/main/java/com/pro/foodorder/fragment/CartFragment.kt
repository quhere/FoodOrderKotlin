package com.pro.foodorder.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.pro.foodorder.ControllerApplication
import com.pro.foodorder.R
import com.pro.foodorder.activity.MainActivity
import com.pro.foodorder.adapter.CartAdapter
import com.pro.foodorder.adapter.CartAdapter.IClickListener
import com.pro.foodorder.constant.Constant
import com.pro.foodorder.constant.GlobalFunction.hideSoftKeyboard
import com.pro.foodorder.constant.GlobalFunction.showToastMessage
import com.pro.foodorder.database.FoodDatabase.Companion.getInstance
import com.pro.foodorder.databinding.FragmentCartBinding
import com.pro.foodorder.event.ReloadListCartEvent
import com.pro.foodorder.model.Food
import com.pro.foodorder.model.Order
import com.pro.foodorder.prefs.DataStoreManager.Companion.user
import com.pro.foodorder.utils.StringUtil.isEmpty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class CartFragment : BaseFragment() {

    private var mFragmentCartBinding: FragmentCartBinding? = null
    private var mCartAdapter: CartAdapter? = null
    private var mListFoodCart: MutableList<Food>? = null
    private var mAmount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mFragmentCartBinding = FragmentCartBinding.inflate(inflater, container, false)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        displayListFoodInCart()
        mFragmentCartBinding!!.tvOrderCart.setOnClickListener { onClickOrderCart() }
        return mFragmentCartBinding!!.root
    }

    override fun initToolbar() {
        if (activity != null) {
            (activity as MainActivity?)!!.setToolBar(false, getString(R.string.cart))
        }
    }

    private fun displayListFoodInCart() {
        if (activity == null) {
            return
        }
        val linearLayoutManager = LinearLayoutManager(activity)
        mFragmentCartBinding!!.rcvFoodCart.layoutManager = linearLayoutManager
        val itemDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        mFragmentCartBinding!!.rcvFoodCart.addItemDecoration(itemDecoration)
        initDataFoodCart()
    }

    private fun initDataFoodCart() {
        mListFoodCart = ArrayList()
        mListFoodCart = getInstance(activity!!)!!.foodDAO()!!.listFoodCart
        if (mListFoodCart == null || mListFoodCart!!.isEmpty()) {
            return
        }
        mCartAdapter = CartAdapter(mListFoodCart, object : IClickListener {
            override fun clickDeteteFood(food: Food?, position: Int) {
                deleteFoodFromCart(food, position)
            }

            override fun updateItemFood(food: Food?, position: Int) {
                getInstance(activity!!)!!.foodDAO()!!.updateFood(food)
                mCartAdapter!!.notifyItemChanged(position)
                calculateTotalPrice()
            }
        })
        mFragmentCartBinding!!.rcvFoodCart.adapter = mCartAdapter
        calculateTotalPrice()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearCart() {
        mListFoodCart?.clear()
        mCartAdapter!!.notifyDataSetChanged()
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        val listFoodCart = getInstance(activity!!)!!.foodDAO()!!.listFoodCart
        if (listFoodCart == null || listFoodCart.isEmpty()) {
            val strZero: String = "" + 0 + Constant.CURRENCY
            mFragmentCartBinding!!.tvTotalPrice.text = strZero
            mAmount = 0
            return
        }
        var totalPrice = 0
        for (food in listFoodCart) {
            totalPrice += food.totalPrice
        }
        val strTotalPrice: String = "" + totalPrice + Constant.CURRENCY
        mFragmentCartBinding!!.tvTotalPrice.text = strTotalPrice
        mAmount = totalPrice
    }

    private fun deleteFoodFromCart(food: Food?, position: Int) {
        AlertDialog.Builder(activity)
                .setTitle(getString(R.string.confirm_delete_food))
                .setMessage(getString(R.string.message_delete_food))
                .setPositiveButton(getString(R.string.delete)) { _: DialogInterface?, _: Int ->
                    getInstance(activity!!)!!.foodDAO()!!.deleteFood(food)
                    mListFoodCart?.removeAt(position)
                    mCartAdapter!!.notifyItemRemoved(position)
                    calculateTotalPrice()
                }
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
    }

    private fun onClickOrderCart() {
        if (activity == null) {
            return
        }
        if (mListFoodCart == null || mListFoodCart!!.isEmpty()) {
            return
        }
        @SuppressLint("InflateParams") val viewDialog = layoutInflater.inflate(R.layout.layout_bottom_sheet_order, null)
        val bottomSheetDialog = BottomSheetDialog(activity!!)
        bottomSheetDialog.setContentView(viewDialog)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // init ui
        val tvFoodsOrder = viewDialog.findViewById<TextView>(R.id.tv_foods_order)
        val tvPriceOrder = viewDialog.findViewById<TextView>(R.id.tv_price_order)
        val edtNameOrder = viewDialog.findViewById<TextView>(R.id.edt_name_order)
        val edtPhoneOrder = viewDialog.findViewById<TextView>(R.id.edt_phone_order)
        val edtAddressOrder = viewDialog.findViewById<TextView>(R.id.edt_address_order)
        val tvCancelOrder = viewDialog.findViewById<TextView>(R.id.tv_cancel_order)
        val tvCreateOrder = viewDialog.findViewById<TextView>(R.id.tv_create_order)

        // Set data
        tvFoodsOrder.text = getStringListFoodsOrder()
        tvPriceOrder.text = mFragmentCartBinding!!.tvTotalPrice.text.toString()

        // Set listener
        tvCancelOrder.setOnClickListener { bottomSheetDialog.dismiss() }
        tvCreateOrder.setOnClickListener {
            val strName = edtNameOrder.text.toString().trim { it <= ' ' }
            val strPhone = edtPhoneOrder.text.toString().trim { it <= ' ' }
            val strAddress = edtAddressOrder.text.toString().trim { it <= ' ' }
            if (isEmpty(strName) || isEmpty(strPhone) || isEmpty(strAddress)) {
                showToastMessage(activity, getString(R.string.message_enter_infor_order))
            } else {
                val id = System.currentTimeMillis()
                val strEmail = user!!.email
                val order = Order(id, strName, strEmail, strPhone, strAddress,
                        mAmount, getStringListFoodsOrder(), Constant.TYPE_PAYMENT_CASH, false)
                ControllerApplication[activity!!].bookingDatabaseReference
                        .child(id.toString())
                        .setValue(order) { _: DatabaseError?, _: DatabaseReference? ->
                            showToastMessage(activity, getString(R.string.msg_order_success))
                            hideSoftKeyboard(activity!!)
                            bottomSheetDialog.dismiss()
                            getInstance(activity!!)!!.foodDAO()!!.deleteAllFood()
                            clearCart()
                        }
            }
        }
        bottomSheetDialog.show()
    }

    private fun getStringListFoodsOrder(): String {
        if (mListFoodCart == null || mListFoodCart!!.isEmpty()) {
            return ""
        }
        var result = ""
        for (food in mListFoodCart!!) {
            result = if (isEmpty(result)) {
                ("- " + food.name + " (" + food.realPrice + Constant.CURRENCY + ") "
                        + "- " + getString(R.string.quantity) + " " + food.count)
            } else {
                (result + "\n" + ("- " + food.name + " (" + food.realPrice + Constant.CURRENCY + ") "
                        + "- " + getString(R.string.quantity) + " " + food.count))

            }
        }
        return result
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ReloadListCartEvent?) {
        displayListFoodInCart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}