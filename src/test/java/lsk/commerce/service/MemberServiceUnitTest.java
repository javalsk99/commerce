package lsk.commerce.service;

import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;


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

    @Nested
    class SuccessCase {

        @Test
        void join() {
            //given
            MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

            given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
            given(memberRepository.existsByLoginId(anyString())).willReturn(false);

            //when
            memberService.join(request);

            //then
            assertAll(
                    () -> then(passwordEncoder).should().encode(anyString()),
                    () -> then(memberRepository).should().existsByLoginId(anyString()),
                    () -> then(memberRepository).should().save(argThat(m ->
                            m.getPassword().equals(encodedPassword) && m.getGrade() == Grade.USER))
            );
        }

        @Test
        void adminJoin() {
            //given
            MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

            given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
            given(memberRepository.existsByLoginId(anyString())).willReturn(false);

            //when
            memberService.adminJoin(request);

            //then
            assertAll(
                    () -> then(passwordEncoder).should().encode(anyString()),
                    () -> then(memberRepository).should().existsByLoginId(anyString()),
                    () -> then(memberRepository).should().save(argThat(m -> m.getGrade() == Grade.ADMIN))
            );
        }

        @Test
        void findByLoginId() {
            //given
            Member member = Member.builder().loginId(loginId).build();

            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

            //when
            memberService.findMemberByLoginId(loginId);

            //then
            then(memberRepository).should().findByLoginId(anyString());
        }

        @Test
        void findForLogin() {
            //given
            Member member = Member.builder().loginId(loginId).build();

            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

            //when
            memberService.findMemberForLogin(loginId);

            //then
            then(memberRepository).should().findByLoginId(anyString());
        }

        @Test
        void changePassword() {
            //given
            Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));
            given(passwordEncoder.encode(anyString())).willReturn(newEncodedPassword);
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            //when
            memberService.changePassword(loginId, "11111111");

            //then
            assertAll(
                    () -> then(memberRepository).should().findByLoginId(anyString()),
                    () -> then(passwordEncoder).should().encode(anyString()),
                    () -> then(passwordEncoder).should().matches(anyString(), anyString()),
                    () -> assertThat(member.getPassword()).isEqualTo(newEncodedPassword)
            );
        }

        @Test
        void changeAddress() {
            //given
            Member member = Member.builder().loginId(loginId).city("Seoul").street("Gangnam").street("01234").build();

            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

            //when
            memberService.changeAddress(loginId, "Gyeonggi-do", "Gangbuk", "01235");

            //then
            assertAll(
                    () -> then(memberRepository).should().findByLoginId(anyString()),
                    () -> assertThat(member.getAddress())
                            .extracting("city", "street", "zipcode")
                            .contains("Gyeonggi-do", "Gangbuk", "01235")
            );
        }

        @Test
        void delete() {
            //given
            Member member = Member.builder().loginId(loginId).build();

            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

            //when
            memberService.deleteMember(loginId);

            //then
            assertAll(
                    () -> then(memberRepository).should().findByLoginId(anyString()),
                    () -> then(memberRepository).should().delete(member)
            );
        }

        @Test
        void changeDto() {
            //given
            Member member = Member.builder().loginId(loginId).build();

            //when
            MemberResponse memberDto = memberService.getMemberDto(member);

            //then
            assertAll(
                    () -> assertThat(memberDto.getLoginId()).isEqualTo(loginId),
                    () -> assertThat(memberDto.getGrade()).isEqualTo(Grade.USER)
            );
        }
    }

    @Nested
    class FailureCase {

        @Test
        void join_existsLoginId() {
            //given
            MemberRequest request = MemberRequest.builder().loginId(loginId).password(rawPassword).build();

            given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
            given(memberRepository.existsByLoginId(anyString())).willReturn(true);

            //when
            assertThatThrownBy(() -> memberService.join(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 아이디입니다.");

            //then
            assertAll(
                    () -> then(passwordEncoder).should().encode(anyString()),
                    () -> then(memberRepository).should().existsByLoginId(anyString()),
                    () -> then(memberRepository).should(never()).save(any())
            );
        }

        @Test
        void join_notExistsPassword() {
            //given
            MemberRequest request1 = MemberRequest.builder().password(null).build();
            MemberRequest request2 = MemberRequest.builder().password(" ").build();

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> memberService.join(request1))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("비밀번호가 비어있습니다."),
                    () -> assertThatThrownBy(() -> memberService.join(request2))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("비밀번호가 비어있습니다.")
            );

            //then
            assertAll(
                    () -> then(passwordEncoder).should(never()).encode(any()),
                    () -> then(memberRepository).should(never()).save(any())
            );
        }

        @Test
        void join_rawPassword() {
            //given
            MemberRequest request = MemberRequest.builder().password(rawPassword).build();

            given(passwordEncoder.encode(anyString())).willReturn(rawPassword);

            //when
            assertThatThrownBy(() -> memberService.join(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("암호화되지 않은 비밀번호입니다.");

            //then
            assertAll(
                    () -> then(passwordEncoder).should().encode(anyString()),
                    () -> then(memberRepository).should(never()).save(any())
            );
        }

        @Test
        void findByLoginId_notExistsMember() {
            //given
            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> memberService.findMemberByLoginId(loginId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 아이디입니다.");

            //then
            then(memberRepository).should().findByLoginId(anyString());
        }

        @Test
        void findForLogin_notExistsMember() {
            //given
            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> memberService.findMemberForLogin(loginId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("아이디 또는 비밀번호가 틀렸습니다.");

            //then
            then(memberRepository).should().findByLoginId(anyString());
        }

        @Test
        void changePassword_notExistsMember() {
            //given
            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> memberService.changePassword(loginId, "11111111"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 아이디입니다.");

            //then
            assertAll(
                    () -> then(memberRepository).should().findByLoginId(anyString()),
                    () -> then(passwordEncoder).should(never()).encode(any()),
                    () -> then(passwordEncoder).should(never()).matches(any(), any())
            );
        }

        @Test
        void delete_notExistsMember() {
            //given
            given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> memberService.deleteMember(loginId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 아이디입니다.");

            //then
            assertAll(
                    () -> then(memberRepository).should().findByLoginId(anyString()),
                    () -> then(memberRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_alreadyDeleted() {
            //given
            Member member = Member.builder().loginId(loginId).build();

            given(memberRepository.findByLoginId(anyString()))
                    .willReturn(Optional.of(member))
                    .willReturn(Optional.empty());

            //when 첫 번째 호출
            memberService.deleteMember(loginId);

            //then
            assertAll(
                    () -> then(memberRepository).should(times(1)).findByLoginId(anyString()),
                    () -> then(memberRepository).should(times(1)).delete(member)
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> memberService.deleteMember(loginId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 아이디입니다.");

            //then
            assertAll(
                    () -> then(memberRepository).should(times(2)).findByLoginId(anyString()),
                    () -> then(memberRepository).should(times(1)).delete(any())
            );
        }
    }
}