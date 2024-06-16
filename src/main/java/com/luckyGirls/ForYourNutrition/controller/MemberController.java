package com.luckyGirls.ForYourNutrition.controller;

import com.luckyGirls.ForYourNutrition.domain.Address;
import com.luckyGirls.ForYourNutrition.domain.Cart;
import com.luckyGirls.ForYourNutrition.domain.Member;
import com.luckyGirls.ForYourNutrition.service.AddressService;
import com.luckyGirls.ForYourNutrition.service.CartService;
import com.luckyGirls.ForYourNutrition.service.MemberService;
import com.luckyGirls.ForYourNutrition.validator.LoginFormValidator;
import com.luckyGirls.ForYourNutrition.validator.MemberFormValidator;
import com.luckyGirls.ForYourNutrition.validator.SearchIdFormValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


@Controller
@SessionAttributes("memberSession")
public class MemberController {
	@Autowired
	private MemberService memberService;
	
	@Autowired
	private Authenticator authenticator;
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private AddressService addressService;

	@ModelAttribute("loginForm")
	public LoginForm formBacking(HttpServletRequest request) throws Exception {
		return new LoginForm();
	}
	
	//로그인 폼
	@GetMapping("/member/loginForm")
	public String loginForm(Model model){
		return "member/loginForm";
	}

	//로그인
	@PostMapping("/member/login")
	public ModelAndView handleRequest(HttpServletRequest request, HttpSession session,
			@ModelAttribute("loginForm") LoginForm loginForm, Model model, BindingResult bindingResult) throws Exception {
		
		new LoginFormValidator().validate(loginForm, bindingResult);

		if (bindingResult.hasErrors()) {
			System.out.println(bindingResult);
			return new ModelAndView("member/loginForm");
		}
		
		Member m = memberService.getMember(loginForm.getId(), loginForm.getPassword());
		
		try {
			authenticator.authenticate(loginForm); // id과 password가 맞는지 검증
			MemberSession memberSession = new MemberSession(m);
			session.setAttribute("ms", memberSession);
			return new ModelAndView("redirect:/main");
		} catch (AuthenticationException e) { // 검증 실패 시
			bindingResult.reject(e.getMessage()); // error message
			return new ModelAndView("member/loginForm");
		}
	}
    
/*	
	@GetMapping("/searchIdForm")
	public String viewSerchIdForm(Model model){
		return "member/searchIdForm";
	}

	@PostMapping("/searchId")
	public ModelAndView handleRequest(HttpServletRequest request, HttpSession session,
			@ModelAttribute("searchIdForm") SearchIdForm searchIdForm, Model model, BindingResult bindingResult) throws Exception {
		new SearchIdFormValidator().validate(searchIdForm, bindingResult);
		
		if (bindingResult.hasErrors()) {
			System.out.println(bindingResult);
			return new ModelAndView("member/searchIdForm");
		}
		
		String id = memberService.findId(searchIdForm.getEmail(), searchIdForm.getName());
		
		return new ModelAndView("member/searchIdForm");
	}

	@RequestMapping(value = "/searchPwd.do", method = RequestMethod.GET)
	public ModelAndView viewSearchPwdForm(HttpServletRequest request) throws Exception {
		//추후 구현
	}

	@RequestMapping(value = "/searchPwd.do", method = RequestMethod.POST)
	public ModelAndView searchPwd(HttpServletRequest request,
			@RequestParam("id") String id,
			@RequestParam("email") String email) throws Exception {
		//추후 구현
	}

	 */

	//로그아웃
	@RequestMapping("/member/logout")
	public String handleRequest(HttpSession session, Model model) throws Exception {
		session.removeAttribute("ms");
		session.invalidate();
		return "redirect:/main";
	}
	
	//회원가입 폼
	@GetMapping("member/join")
	public String joinForm(Model model, HttpSession session) {
		model.addAttribute("memberForm", new MemberForm());
		return "member/joinForm";
	}
	
	//회원가입
	@PostMapping("member/join")
	public String join(HttpServletRequest request, HttpSession session,
			@ModelAttribute("memberForm") MemberForm memberForm, BindingResult result, Model model) throws Exception {
		new MemberFormValidator().validate(memberForm, result);
		
		if (memberService.getMember(memberForm.getMember().getId()) != null) {
			result.reject("sameIdExist", new Object[] {}, null);
			return "member/joinForm";
		}
		
		if (result.hasErrors()) {
			return "member/joinForm";
		} else {
			memberService.insertMember(memberForm.getMember());
			
			Member new_m = memberService.getMember(memberForm.getMember().getId());
			
			Address address = memberForm.getAddress();
			address.setMember(new_m);
			addressService.insertAddress(address);

			//회원가입 시 자동으로 cart 생성
			Cart cart = new Cart();
			cart.setMember(new_m);;
			cartService.createCart(new_m);
			model.addAttribute("loginForm", new LoginForm());
			
			return "member/loginForm";
		}
	}

	//회원 수정 폼
	@GetMapping("member/modifyMember")
	public String updateForm(Model model, HttpSession session) {
		try {
			MemberSession ms = (MemberSession)session.getAttribute("ms");
			Member member = ms.getMember();
			MemberForm memberForm = new MemberForm();
			memberForm.setMember(member);
			model.addAttribute("memberForm", memberForm);
			return "member/updateForm";
		}
		catch (NullPointerException ex) {
			model.addAttribute("memberForm", new MemberForm());
			return "member/loginForm";
		}
	}
	
	//회원 수정
	@PostMapping("member/modifyMember")
	public String modifyMember(HttpServletRequest request, HttpSession session,
			@ModelAttribute("memberForm") MemberForm memberForm, BindingResult result, Model model) throws Exception {
		new MemberFormValidator().validate(memberForm, result);
		
		if (result.hasErrors()) {
			return "member/updateForm";
		} else {
			memberService.updateMember(memberForm.getMember());
			Member m = memberService.getMember(memberForm.getMember().getId());
			MemberSession memberSession = new MemberSession(m);
			session.setAttribute("ms", memberSession);
			System.out.println(memberSession.getMember().getId());
			
			return "redirect:/member/memberDetail";
		}
	}
	
	//회원 삭제
	@GetMapping("member/delete")
	public String deleteMember(HttpSession session) throws Exception {
		MemberSession memberSession = (MemberSession) session.getAttribute("ms");
		memberService.deleteMember(memberSession.getMember().getId());
		session.removeAttribute("memberSession");
		session.invalidate();
		return "redirect:/member/loginForm";
	}
	
	//회원 정보 조회
	@GetMapping("/member/memberDetail")
	public ModelAndView memberDetail(HttpSession session, Model model) throws Exception {
		try {
			MemberSession ms = (MemberSession)session.getAttribute("ms");
			Member member = ms.getMember();
			model.addAttribute("member", member);
			return new ModelAndView("/member/memberDetail");
		}
		catch (NullPointerException ex) {
			model.addAttribute("member", new Member());
			return new ModelAndView("member/loginForm");
		}
	}
	
	//마이페이지(상세)
	@GetMapping("/member/memberInfo")
	public String memberInfo(HttpSession session, Model model){
		return "member/memberInfo";
	}
	
	//마이페이지
	@GetMapping("/member/myPage")
	public ModelAndView myPage(HttpSession session, Model model){
		try {
			MemberSession ms = (MemberSession)session.getAttribute("ms");
			Member member = ms.getMember();
			model.addAttribute("member", member);
			return new ModelAndView("/member/myPage");
		}
		catch (NullPointerException ex) {
			model.addAttribute("member", new Member());
			return new ModelAndView("member/loginForm");
		}
	}
	
	@GetMapping("/header")
	public String getHeader(Model model, HttpSession session) {
		return "header";
	}
	
	@GetMapping({"/main", "/"})
	public String getMain(Model model, HttpSession session) {
		return "main";
	}
}
