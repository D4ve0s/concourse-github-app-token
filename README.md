# concourse-github-app-token
[![Docker Image Size (tag)](https://img.shields.io/docker/image-size/tenjaa/concourse-github-app-token/latest?style=flat-square)](https://hub.docker.com/r/tenjaa/concourse-github-app-token)

## What?
This concourse resource gets you an installation token for your GitHub App.

## Why?
Accessing the GitHub API typically requires some authentication, like [setting a commit status](https://docs.github.com/en/rest/reference/repos#statuses).
Using a shared [machine user](https://developer.github.com/v3/guides/managing-deploy-keys/#machine-users) might not always be wanted or possible (e.g. GitHub Enterprise restrictions).

Also, I just wanted to use GraalVM and needed a project with actual use :)

## Example
```yaml
resource_types:
- name: github-token-resource
  type: registry-image
  source:
    repository: tenjaa/concourse-github-app-token # or
    repository: public.ecr.aws/tenjaa/concourse-github-app-token # or
    repository: ghcr.io/tenjaa/concourse-github-app-token

resources:
- name: github-token
  type: github-token-resource
  source:
    appId: ((github-app-id)) # can be looked up on the overview page of your app
    privateKey: ((github-app-private-key)) # https://docs.github.com/en/developers/apps/authenticating-with-github-apps#generating-a-private-key
    user: tenjaa # get a token for an app installed to a user account
      # OR
    org: my-org # get a token for an app installed to an org account

    # Optional: downscope the installation token (least-privilege). Omit both to keep the app's full grants.
    repositories: # restrict the token to these repos (must be installed for the app)
    - repo-a
    - repo-b
    permissions: # restrict the token to these permission levels (read / write / admin)
      contents: read
      pull_requests: write

jobs:
- name: my-job
  plan:
  - put: github-token # put forces to get a new token, even when rerunning a build (https://concourse-ci.org/builds.html#build-rerunning)
  - load_var: token
    file: github-token/token
  - task: print-env
    config:
      platform: linux
      image_resource:
        type: registry-image
        source: { repository: alpine }
      params:
        TOKEN: ((.:token))
      run:
        path: env
```

### Scoping via `get` params (per-fetch)

The same `repositories` / `permissions` keys can be supplied per `get` step instead of in `source:`. This is the idiomatic place for a *per-fetch* concern: one resource definition can mint differently-scoped tokens for different jobs without redefining the resource. `get` params take precedence over `source:` when both are set.

```yaml
jobs:
- name: read-only-job
  plan:
  - get: github-token
    params:
      repositories:
      - repo-a
      permissions:
        contents: read
```

## Future use
A combination of [var sources](https://github.com/concourse/rfcs/blob/master/039-var-sources/proposal.md) and [prototypes](https://github.com/concourse/rfcs/blob/master/037-prototypes/proposal.md) will greatly improve the usability of this resource.

## Links
- https://docs.github.com/en/developers/apps
- https://docs.github.com/en/rest/reference/apps#create-an-installation-access-token-for-an-app
- https://developer.github.com/changes/2020-02-14-deprecating-password-auth/
