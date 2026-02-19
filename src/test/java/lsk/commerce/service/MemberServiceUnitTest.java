package lsk.commerce.service;

import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;


@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberService memberService;

    String loginId = "id_A";
    String rawPassword = "12345678";
    String encodedPassword = "$2a$diioffd783294fkdj";
    String newEncodedPassword = "$2a$jgio2383dshnj987";

    @Test
    void join() {
        //given
        MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

        given(passwordEncoder.encode(any())).willReturn(encodedPassword);
        given(memberRepository.existsByLoginId(any())).willReturn(false);

        //when
        memberService.join(request);

        //then
        then(passwordEncoder).should().encode(rawPassword);
        then(memberRepository).should().existsByLoginId(any());
        then(memberRepository).should().save(any());
        then(memberRepository).should().save(argThat(m ->
                m.getPassword().equals(encodedPassword) &&
                m.getGrade() == Grade.USER));
    }

    @Test
    void adminJoin() {
        //given
        MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

        given(passwordEncoder.encode(any())).willReturn(encodedPassword);
        given(memberRepository.existsByLoginId(any())).willReturn(false);

        //when
        memberService.adminJoin(request);

        //then
        then(passwordEncoder).should().encode(rawPassword);
        then(memberRepository).should().existsByLoginId(any());
        then(memberRepository).should().save(argThat(m -> m.getGrade() == Grade.ADMIN));
    }

    @Test
    void failed_join_existsLoginId() {
        //given
        MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

        given(passwordEncoder.encode(any())).willReturn(encodedPassword);
        given(memberRepository.existsByLoginId(any())).willReturn(true);

        //when
        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");

        //then
        then(memberRepository).should(never()).save(any());
    }

    @Test
    void failed_join_notExistsPassword() {
        //given
        MemberRequest request1 = MemberRequest.builder().password(null).build();
        MemberRequest request2 = MemberRequest.builder().password(" ").build();

        //when
        assertThatThrownBy(() -> memberService.join(request1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 비어있습니다.");
        assertThatThrownBy(() -> memberService.join(request2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 비어있습니다.");

        //then
        then(passwordEncoder).should(never()).encode(any());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    void failed_join_rawPassword() {
        //given
        MemberRequest request = MemberRequest.builder().password(rawPassword).build();

        given(passwordEncoder.encode(any())).willReturn(rawPassword);

        //when
        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("암호화되지 않은 비밀번호입니다.");

        //then
        then(passwordEncoder).should().encode(any());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    void findByLoginId() {
        //given
        Member member = Member.builder().loginId(loginId).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));

        //when
        memberService.findMemberByLoginId(loginId);

        //then
        then(memberRepository).should().findByLoginId(any());
    }

    @Test
    void failed_findByLoginId() {
        //given
        given(memberRepository.findByLoginId(any())).willReturn(Optional.empty());

        //when
        assertThatThrownBy(() -> memberService.findMemberByLoginId(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
    }

    @Test
    void findForLogin() {
        //given
        Member member = Member.builder().loginId(loginId).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));

        //when
        memberService.findMemberForLogin(loginId);

        //then
        then(memberRepository).should().findByLoginId(any());
    }

    @Test
    void failed_findForLogin() {
        //given
        given(memberRepository.findByLoginId(any())).willReturn(Optional.empty());

        //when
        assertThatThrownBy(() -> memberService.findMemberForLogin(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 또는 비밀번호가 틀렸습니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
    }

    @Test
    void changePassword() {
        //given
        Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));
        given(passwordEncoder.encode(any())).willReturn(newEncodedPassword);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        //when
        memberService.changePassword(loginId, "11111111");

        //then
        then(memberRepository).should().findByLoginId(any());
        then(passwordEncoder).should().encode(any());
        then(passwordEncoder).should().matches(anyString(), anyString());
        assertThat(member.getPassword()).isEqualTo(newEncodedPassword);
    }

    @Test
    void failed_changePassword_notExistsMember() {
        //given
        given(memberRepository.findByLoginId(any())).willReturn(Optional.empty());

        //when
        assertThatThrownBy(() -> memberService.changePassword(loginId, "11111111"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
        then(passwordEncoder).should(never()).encode(any());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
    }

    @Test
    void failed_changePassword_notExistsPassword() {
        //given
        Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

        //when
        assertThatThrownBy(() -> memberService.changePassword(loginId, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 비어있습니다.");

        //then
        then(memberRepository).should(never()).findByLoginId(any());
        then(passwordEncoder).should(never()).encode(any());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        assertThat(member.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    void failed_changePassword_rawPassword() {
        //given
        Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));
        given(passwordEncoder.encode(any())).willReturn("11111111");

        //when
        assertThatThrownBy(() -> memberService.changePassword(loginId, "11111111"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("암호화되지 않은 비밀번호입니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
        then(passwordEncoder).should().encode(any());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        assertThat(member.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    void failed_changePassword_samePassword() {
        //given
        Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));
        given(passwordEncoder.encode(any())).willReturn(encodedPassword);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        //when
        assertThatThrownBy(() -> memberService.changePassword(loginId, "11111111"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 기존과 달라야 합니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
        then(passwordEncoder).should().encode(any());
        then(passwordEncoder).should().matches(anyString(), anyString());
    }

    @Test
    void changeAddress() {
        //given
        Member member = Member.builder().loginId(loginId).city("Seoul").street("Gangnam").street("01234").build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));

        //when
        memberService.changeAddress(loginId, "Gyeonggi-do", "Gangbuk", "01235");

        //then
        then(memberRepository).should().findByLoginId(any());
        assertThat(member.getAddress())
                .extracting("city", "street", "zipcode")
                .contains("Gyeonggi-do", "Gangbuk", "01235");
    }

    @Test
    void failed_changeAddress_sameAddress() {
        //given
        Member member = Member.builder().loginId(loginId).city("Seoul").street("Gangnam").zipcode("01234").build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));

        //when
        assertThatThrownBy(() -> memberService.changeAddress(loginId, "Seoul", "Gangnam", "01234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주소가 기존과 달라야 합니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
    }

    @Test
    void delete() {
        //given
        Member member = Member.builder().loginId(loginId).build();

        given(memberRepository.findByLoginId(any())).willReturn(Optional.of(member));

        //when
        memberService.deleteMember(loginId);

        //then
        then(memberRepository).should().findByLoginId(any());
        then(memberRepository).should().delete(any());
    }

    @Test
    void failed_delete_notExistsMember() {
        //given
        given(memberRepository.findByLoginId(any())).willReturn(Optional.empty());

        //when
        assertThatThrownBy(() -> memberService.deleteMember(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");

        //then
        then(memberRepository).should().findByLoginId(any());
        then(memberRepository).should(never()).delete(any());
    }

    @Test
    void changeDto() {
        //given
        Member member = Member.builder().loginId(loginId).build();

        //when
        MemberResponse memberDto = memberService.getMemberDto(member);

        //then
        Assertions.assertAll(
                () -> assertThat(memberDto.getLoginId()).isEqualTo(loginId),
                () -> assertThat(memberDto.getGrade()).isEqualTo(Grade.USER)
        );
    }
}