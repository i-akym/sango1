# Sango

## OVERVIEW
'Sango'は，Isao Akiyamaが趣味で開発している，以下の特徴をもつプログラミング言語です。
  - 関数型言語，遅延評価でなく即時評価
  - オブジェクトは不変（immutable）
  - 静的な強い型付け
  - 関数名やデータ構築子をパラメタの後ろに書く，つまり書いてある順番どおりに実行される
  - メッセージ交換による並行処理

## LICENSE
MIT License

## REQUIREMENTS
Java 1.8 or higher

## DOWNLOAD RELEASE PACKAGE
[here](https://github.com/i-akym/sango-release)

## DOCUMENTS
src/doc/*.html

## SAMPLE CODE
Tower of Hanoi

    import "sango.cstr" -> cstr ;;
    import "sango.debug" -> debug ;;
    import "sango.io.stdio" -> stdio ;;
    import "sango.num.int" -> int ;;
    import "sango.system.runtime" -> runtime ;;
    
    data <move> :=
    | <int>  # disk number; top is 1
      <cstr> # from
      <cstr> # to
      single$
    | <int> # number of disks
      <cstr> # from
      <cstr> # via
      <cstr> # to
      multiple$ ;;
    
    eval _main_ -> <void> {
      runtime.args = [ *N ; ** ],
      N int.parse = *Disks,
      if {
      ; Disks 1 lt? -> bad_arg$ "Bad number of disks." new_exception >> throw
      ; otherwise ->
      },
      [ Disks "A" "C" "B" multiple$ ] &\ <move list> *Ms -> <void> {
        Ms case {
        ; [ 0 ** ** ** multiple$ ; *Ms' ] -> Ms' &&
        ; [ *X *F *V *T multiple$ ; *Ms' ] ->
          [ (X dec) F T V multiple$, X F T single$, (X dec) V F T multiple$ ; Ms' ] &&
        ; [ *X *F *T single$ ; *Ms' ] ->
          [ "Move ", X int.format, " from ", F, " to ", T, "." ] cstr.concat >> stdio.print_and_newline,
          Ms' &&
        ; [] ->
        }
      },
      stdio.flush
    } ;;
