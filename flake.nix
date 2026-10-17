{
  description = "Fluxnautics dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      pkgsFor = forAllSystems (system: nixpkgs.legacyPackages.${system});
    in
    {
      formatter = forAllSystems (system: pkgsFor.${system}.nixfmt-rfc-style);

      devShells = forAllSystems (
        system:
        let
          pkgs = pkgsFor.${system};
          lib = pkgs.lib;
          inherit (pkgs) stdenv;

          jdk = pkgs.temurin-bin-21;

          glLibs = with pkgs; [
            libGL
            libpulseaudio
            libX11
            libXext
            libXrender
            libXcursor
            libXrandr
            libXxf86vm
            libXinerama
            libXi
            libxcb
            libXau
            libXdmcp
          ];
        in
        {
          default = pkgs.mkShellNoCC {
            name = "fluxnautics";

            packages = [
              jdk
              pkgs.git
            ] ++ lib.optionals stdenv.isLinux glLibs;

            JAVA_HOME = "${jdk.home}";

            GRADLE_OPTS = "-Dorg.gradle.java.installations.paths=${jdk.home} -Dorg.gradle.java.installations.auto-download=false";
            LD_LIBRARY_PATH = lib.optionalString stdenv.isLinux (lib.makeLibraryPath glLibs);

            shellHook = ''
              echo "Fluxnautics dev shell"
              echo "Java     : $(java -version 2>&1 | head -n 1)"
              echo "JAVA_HOME: $JAVA_HOME"
              echo ""
              echo "Gradle (wrapper 9.8.0 is authoritative):"
              echo "  ./gradlew build              compile + jar to build/libs"
              echo "  ./gradlew runClient          dev client (run/client)"
              echo "  ./gradlew runServer          headless dev server (run/server)"
              echo "  ./gradlew runData            regenerate src/generated/resources"
            '';
          };
        }
        // lib.optionalAttrs stdenv.isLinux {
          fhs = (pkgs.buildFHSEnv {
            name = "fluxnautics-fhs";
            targetPkgs = p: [
              p.temurin-bin-21
              p.git
            ] ++ glLibs;
            profile = ''
              export JAVA_HOME=${jdk.home}
              export GRADLE_OPTS="-Dorg.gradle.java.installations.paths=${jdk.home} -Dorg.gradle.java.installations.auto-download=false"
            '';
            runScript = "${pkgs.bashInteractive}/bin/bash";
          }).env;
        }
      );
    };
}
