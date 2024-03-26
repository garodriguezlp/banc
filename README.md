# banc

```txt
8 888888888o          .8.          b.             8     ,o888888o.
8 8888    `88.       .888.         888o.          8    8888     `88.
8 8888     `88      :88888.        Y88888o.       8 ,8 8888       `8.
8 8888     ,88     . `88888.       .`Y888888o.    8 88 8888
8 8888.   ,88'    .8. `88888.      8o. `Y888888o. 8 88 8888
8 8888888888     .8`8. `88888.     8`Y8o. `Y88888o8 88 8888
8 8888    `88.  .8' `8. `88888.    8   `Y8o. `Y8888 88 8888
8 8888      88 .8'   `8. `88888.   8      `Y8o. `Y8 `8 8888       .8'
8 8888    ,88'.888888888. `88888.  8         `Y8o.`    8888     ,88'
8 888888888P .8'       `8. `88888. 8            `Yo     `8888888P'
```

## What is `banc`?

`banc` is a CLI tool built with Java and JBang for transforming financial data from various institutions into a standardized format
for spreadsheet analysis. It processes data from CSV files or table formats, outputting the transformed data to the clipboard for
easy use. Sample files like [`peoplepass.csv`](./peoplepass.csv) and [`bancolombia.csv`](./bancolombia.csv) are provided in the
repository for reference.

## How Does the `banc` Tool Work?

To use `banc`, execute one of the following commands in the tool's directory, depending on your data source:

- For [Peoplepass](https://peoplepass.com.co/) data:

  ```bash
  banc -p peoplepass.csv
  ```

- For [Bancolombia](https://www.bancolombia.com/personas) data:

  ```bash
  banc -b bancolombia.csv
  ```

The tool automatically processes the data and places the output in the clipboard.

## How to Install `banc`

Ensure JBang is installed on your system, then install `banc` with:

```bash
jbang app install banc@garodriguezlp
```

This command installs `banc`, making it ready for use. For more details, including additional commands, refer to the repository
documentation or run `banc --help`.

Before using `banc`, ensure you have the necessary data files, such as `peoplepass.csv` or `bancolombia.csv`. Samples are available
in the repository.

## Try `banc` Without Installing

If you want to try `banc` without installing it or having JBang, you can run it directly using the following one-liner scripts:

- **On Bash (Linux/macOS):**

  ```bash
  curl -Ls https://sh.jbang.dev | bash -s - banc@garodriguezlp --help
  ```

- **On Windows Powershell:**

  ```powershell
  iex "& { $(iwr -useb https://ps.jbang.dev) } banc@garodriguezlp --help"
  ```

These commands will download and execute `banc` directly, allowing you to use the `--help` option to learn more about how to use the
tool.
