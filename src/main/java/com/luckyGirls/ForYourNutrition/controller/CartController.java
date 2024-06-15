package com.luckyGirls.ForYourNutrition.controller;

import com.luckyGirls.ForYourNutrition.domain.Cart;
import com.luckyGirls.ForYourNutrition.domain.CartItem;
import com.luckyGirls.ForYourNutrition.domain.Item;
import com.luckyGirls.ForYourNutrition.domain.Member;
import com.luckyGirls.ForYourNutrition.service.CartService;
import com.luckyGirls.ForYourNutrition.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;
    
    @Autowired
    private ItemService itemService;

    @GetMapping("/cart/viewCart")
    public String viewCart(HttpSession session, Model model) {
        MemberSession ms = (MemberSession) session.getAttribute("ms");
        if (ms == null) {
        	return "redirect:/member/loginForm.do";
        }
        Member member = ms.getMember();
        Cart cart = null;
        try {
            cart = cartService.getCartByMember(member);
            if (cart != null) {
                System.out.println(cart.toString());
            } else {
                System.out.println("Cart is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("cart", cart);
        return "cart/viewCart";
    }

    @PostMapping("/cart/addCartItem")
    public String addCartItem(@RequestParam int item_id, @RequestParam int quantity, HttpSession session, Model model) {
        MemberSession ms = (MemberSession) session.getAttribute("ms");
        if (ms == null) {
            return "redirect:/member/loginForm.do";
        }

        Member member = ms.getMember();
        Item item = itemService.getItemById(item_id);

        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setMember(member);
        cartItem.setQuantity(quantity);
        cartService.addCartItem(member, cartItem);

        Cart cart = cartService.getCartByMember(member);
        model.addAttribute("cart", cart);
        return "redirect:/cart/viewCart";
    }

    @PostMapping("/cart/removeCartItem")
    public String removeCartItem(@RequestParam int cartItem_id, HttpSession session) {
        MemberSession ms = (MemberSession) session.getAttribute("ms");
        if (ms == null) {
        	return "redirect:/member/loginForm.do";
        }
        Member member = ms.getMember();
        cartService.removeCartItem(member, cartItem_id);
        return "redirect:/cart/viewCart";
    }

    @PostMapping("/cart/updateQuantity")
    public String updateQuantity(@RequestParam int cartItem_id, @RequestParam int quantity, @RequestParam String action, HttpSession session) {
        MemberSession ms = (MemberSession) session.getAttribute("ms");
        if (ms == null) {
        	return "redirect:/member/loginForm.do";
        }
        Member member = ms.getMember();
        if ("add".equals(action)) {
            cartService.addQuantity(member, cartItem_id, quantity);
        } else if ("remove".equals(action)) {
            cartService.removeQuantity(member, cartItem_id, quantity);
        } else if ("delete".equals(action)) {
            cartService.removeCartItem(member, cartItem_id);
        }
        return "redirect:/cart/viewCart";
    }

    @GetMapping("/order/createOrder")
    public String createOrderForm(HttpSession session, Model model) {
        MemberSession ms = (MemberSession) session.getAttribute("ms");
        if (ms == null) {
        	return "redirect:/member/loginForm.do";
        }
        Member member = ms.getMember();
        Cart cart = cartService.getCartByMember(member);
        
        if(cart != null) {
        	System.out.println("cartId : " + cart.getCart_id());
        	System.out.println("CartItemList : ");
            for (CartItem cartItem : cart.getCartItems()) {
                System.out.println(" - Item: " + cartItem.getItem().getName() + ", Quantity: " + cartItem.getQuantity());
            }
        } else {
            System.out.println("Cart is null");
        }
        model.addAttribute("cart", cart);
        return "cart/fromCartToOrder";
    }
 
}
